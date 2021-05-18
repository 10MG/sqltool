package cn.tenmg.sqltool.sql.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

public abstract class SQLUtils {

	private static final String WITH = "WITH", SELECT = "SELECT", FROM = "FROM", FROM_REVERSE = "MORF",
			ON_REVERSE = "NO", WHERE_REVERSE = "EREHW", GROUP_REVERSE = "PUORG", ORDER_REVERSE = "REDRO",
			BY_REVERSE = "YB", LIMIT_REVERSE = "TIMIL", OFFSET_REVERSE = "TESFFO",
			/**
			 * 最小标准SQL
			 */
			MIN_SQL = "SELECT * FROM T", SELECT_SQL_TPL = "SELECT %s FROM %s%s", SPACE_WHERE_SPACE = " WHERE ";

	/**
	 * 最小标准SQL长度
	 */
	private static final int MIN_SQL_LEN = MIN_SQL.length(), WITH_LEN = WITH.length(), SELECT_LEN = SELECT.length(),
			FROM_LEN = FROM.length(),

			/**
			 * 含有GROUP BY、ORDER BY、LIMIT子句的标准SQL最小可能长度
			 */
			MIN_LEN = MIN_SQL.concat(" LIMIT 1").length();

	private static final char BLANK_SPACE = '\u0020', LEFT_BRACKET = '\u0028', RIGHT_BRACKET = '\u0029', COMMA = ',',
			SINGLE_QUOTATION_MARK = '\'', LINE_SEPARATOR[] = { '\r', '\n' };

	private static final class DMLCacheHolder {
		private static volatile Map<String, DML> CACHE = new HashMap<String, DML>();
	}

	public static DML getCachedDML(String key) {
		return DMLCacheHolder.CACHE.get(key);
	}

	public static synchronized void cacheDML(String key, DML dml) {
		DMLCacheHolder.CACHE.put(key, dml);
	}

	public static <T> SQL parseSelect(T obj) {
		StringBuilder columns = new StringBuilder(), criteria = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		boolean hasColumn = false, hasWhere = false;
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		try {
			if (entityMeta == null) {
				Class<?> current = type;
				Set<String> fieldSet = new HashSet<String>();
				List<Field> fields = new ArrayList<Field>();
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				while (!Object.class.equals(current)) {
					Field field, declaredFields[] = current.getDeclaredFields();
					for (int i = 0; i < declaredFields.length; i++) {
						field = declaredFields[i];
						String fieldName = field.getName();
						if (!fieldSet.contains(fieldName)) {
							fieldSet.add(fieldName);
							Column column = field.getAnnotation(Column.class);
							if (column != null) {
								field.setAccessible(true);
								fields.add(field);
								String columnName = column.name();
								if (StringUtils.isBlank(columnName)) {
									columnName = StringUtils.camelToUnderline(fieldName, true);
								}
								FieldMeta fieldMeta = new FieldMeta(field, columnName);
								if (field.getAnnotation(Id.class) == null) {
									fieldMeta.setId(false);
								} else {
									fieldMeta.setId(true);
								}
								Object param = field.get(obj);
								if (param != null) {
									params.add(param);
									if (hasWhere) {
										criteria.append(JdbcUtils.SPACE_AND_SPACE);
									} else {
										hasWhere = true;
										criteria.append(SPACE_WHERE_SPACE);
									}
									criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE)
											.append(JdbcUtils.PARAM_MARK);
								}
								if (hasColumn) {
									columns.append(JdbcUtils.COMMA_SPACE);
								} else {
									hasColumn = true;
								}
								columns.append(columnName);

								fieldMetas.add(fieldMeta);
							}
						}
					}
					current = current.getSuperclass();
				}
				EntityUtils.cacheEntityMeta(type, new EntityMeta(EntityUtils.getTableName(type), fieldMetas));
			} else {
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				FieldMeta fieldMeta;
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Object param = fieldMeta.getField().get(obj);
					if (param != null) {
						params.add(param);
						if (hasWhere) {
							criteria.append(JdbcUtils.SPACE_AND_SPACE);
						} else {
							hasWhere = true;
							criteria.append(SPACE_WHERE_SPACE);
						}
						criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.PARAM_MARK);
					}
					if (hasColumn) {
						columns.append(JdbcUtils.COMMA_SPACE);
					} else {
						hasColumn = true;
					}
					columns.append(columnName);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (hasColumn) {
			return new SQL(String.format(SELECT_SQL_TPL, columns, EntityUtils.getTableName(type), criteria), params);
		} else {
			throw new PkNotFoundException(
					"Column not found in class ".concat(type.getName()).concat(", please use @Column to config"));
		}
	}

	/**
	 * 获取SQL相关数据（不对SQL做null校验）
	 * 
	 * @param sql
	 *            SQL
	 * @return 返回SQL相关数据对象
	 */
	public static SQLMetaData getSqlMetaData(String sql) {
		SQLMetaData sqlMetaData = new SQLMetaData();
		int len = sql.length();
		sqlMetaData.setLength(len);
		if (len <= MIN_SQL_LEN) {
			return sqlMetaData;
		}
		if (len >= MIN_LEN) {
			rightAnalysis(sql, len, sqlMetaData);
		}
		leftAnalysis(sql, len, sqlMetaData);
		return sqlMetaData;
	}

	/**
	 * 根据指定的三个前后相邻字符a、b和c，判断其是否为SQL字符串区的结束位置
	 * 
	 * @param a
	 *            前第二个字符a
	 * @param b
	 *            前一个字符b
	 * @param c
	 *            当前字符c
	 * @return 是SQL字符串区域结束位置返回true，否则返回false
	 */
	public static boolean isStringEnd(char a, char b, char c) {
		return (a == SINGLE_QUOTATION_MARK || (a != SINGLE_QUOTATION_MARK && b != SINGLE_QUOTATION_MARK))
				&& c == SINGLE_QUOTATION_MARK;
	}

	/**
	 * 分析SQL右边部分
	 * 
	 * @param sql
	 *            SQL
	 * @param len
	 *            SQL长度
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 */
	private static void rightAnalysis(String sql, int len, SQLMetaData sqlMetaData) {
		int i = len - 1;
		char c = sql.charAt(i);
		boolean isString = false;
		int deep = 0, lineSplitorIndexs[] = { len, len };
		StringBuilder sba = new StringBuilder(), sbb = new StringBuilder();
		while (i > 0 && c <= BLANK_SPACE) {// 跳过空白字符
			decideLineSplitorIndex(lineSplitorIndexs, c, i);
			c = sql.charAt(--i);
		}
		setEmbedEndIndex(sqlMetaData, lineSplitorIndexs[0], lineSplitorIndexs[1]);
		while (i > 0) {
			if (isString) {
				if (i > 2) {
					char b = sql.charAt(--i);
					if (i > 0 && isStringEnd(sql.charAt(i - 1), b, c)) {// 字符串区域结束
						isString = false;
					}
					c = b;
				} else {
					break;
				}
			} else {
				if (c == SINGLE_QUOTATION_MARK) {// 字符串区域开始（这里是倒序）
					isString = true;
					c = sql.charAt(--i);
				} else {
					if (c == RIGHT_BRACKET) {// 右括号
						deep++;
						sba.setLength(0);
						sba.setLength(0);
					} else if (c == LEFT_BRACKET) {// 左括号
						deep--;
						sba.setLength(0);
						sba.setLength(0);
					} else if (deep == 0) {// 深度为0，表示主查询
						if (c == COMMA) {// 逗号
							sba.setLength(0);
							sba.setLength(0);
						} else if (c <= BLANK_SPACE) {// 遇到空白字符
							String sa = sba.toString(), sb = sbb.toString();
							if (i >= MIN_SQL_LEN) {
								if (BY_REVERSE.equalsIgnoreCase(sa)) {
									if (GROUP_REVERSE.equalsIgnoreCase(sb)) {
										sqlMetaData.setGroupByIndex(i + 1);
										break;
									} else if (ORDER_REVERSE.equalsIgnoreCase(sb)) {
										sqlMetaData.setOrderByIndex(i + 1);
									}
								} else if (LIMIT_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setLimitIndex(i + 1);
								} else if (OFFSET_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setOffsetIndex(i + 1);
								} else if (WHERE_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setWhereIndex(i + 1);
									break;
								} else if (ON_REVERSE.equalsIgnoreCase(sb)) {
									break;
								} else if (FROM_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setFromIndex(i + 1);
									break;
								}
							} else if (WHERE_REVERSE.equalsIgnoreCase(sb)) {
								sqlMetaData.setWhereIndex(i + 1);
								break;
							} else if (ON_REVERSE.equalsIgnoreCase(sb)) {
								break;
							} else if (FROM_REVERSE.equalsIgnoreCase(sb)) {
								sqlMetaData.setFromIndex(i + 1);
								break;
							}
							sba = sbb;
							sbb = new StringBuilder();
						} else {
							sbb.append(c);// 拼接单词
						}
					}
					c = sql.charAt(--i);
				}
			}
		}
	}

	/**
	 * 分析SQL左边部分
	 * 
	 * @param sql
	 *            SQL
	 * @param len
	 *            SQL长度
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 */
	private static void leftAnalysis(String sql, int len, SQLMetaData sqlMetaData) {
		int i = 0, deep = 0, max = len, fromIndex = sqlMetaData.getFromIndex(),
				whereIndex = sqlMetaData.getWhereIndex();
		if (whereIndex > 0) {// 含有WHERE子句，只需扫描到WHERE之前即可
			max = whereIndex;
		} else if (fromIndex > 0) {// 没有WHERE子句，但有FROM子句，只需扫描到FROM之前即可
			max = fromIndex;
		}
		int[] lineSplitorIndexs = { -1, -1 };
		char[] charsBefore = { BLANK_SPACE, BLANK_SPACE };
		boolean isString = false, isWith = false;// 是否子字符串区域，是否在WITH子句区域
		StringBuilder sb = new StringBuilder();
		while (i < max) {
			char c = sql.charAt(i);
			if (isWith) {
				if (isString) {
					if (isStringEnd(charsBefore[0], charsBefore[1], c)) {// 字符串区域结束
						isString = false;
					}
					i = stepForward(charsBefore, c, i);
				} else {
					if (c == SINGLE_QUOTATION_MARK) {// 字符串区域开始
						isString = true;
						i = stepForward(charsBefore, c, i);
					} else {
						if (c == LEFT_BRACKET) {// 左括号
							deep++;
						} else if (c == RIGHT_BRACKET) {// 右括号
							deep--;
							if (deep == 0) {
								sb.setLength(0);
							} else if (deep < 0) {
								break;
							}
						} else if (c <= BLANK_SPACE) {// 遇到空白字符
							if (deep == 0) {
								decideLineSplitorIndex(lineSplitorIndexs, c, i);
								String s = sb.toString();
								if (SELECT.equalsIgnoreCase(s)) {
									sqlMetaData.setSelectIndex(i - SELECT_LEN);
									isWith = false;
								}
							}
							sb.setLength(0);
						} else {
							sb.append(c);// 拼接单词
						}
						i = stepForward(charsBefore, c, i);
					}
				}
			} else if (isString) {
				if (isStringEnd(charsBefore[0], charsBefore[1], c)) {// 字符串区域结束
					isString = false;
				}
				i = stepForward(charsBefore, c, i);
			} else {
				if (c == SINGLE_QUOTATION_MARK) {// 字符串区域开始
					isString = true;
					i = stepForward(charsBefore, c, i);
				} else {
					if (c == LEFT_BRACKET) {// 左括号
						deep++;
					} else if (c == RIGHT_BRACKET) {// 右括号
						deep--;
						if (deep == 0) {
							sb.setLength(0);
						} else if (deep < 0) {
							break;
						}
					} else if (c <= BLANK_SPACE) {// 遇到空白字符
						if (deep == 0) {
							decideLineSplitorIndex(lineSplitorIndexs, c, i);
							String s = sb.toString();
							if (SELECT.equalsIgnoreCase(s)) {
								sqlMetaData.setSelectIndex(i - SELECT_LEN);
							} else if (FROM.equalsIgnoreCase(s)) {
								sqlMetaData.setFromIndex(i - FROM_LEN);
							} else if (WITH.equalsIgnoreCase(s)) {
								sqlMetaData.setWithIndex(i - WITH_LEN);
								isWith = true;
							}
						}
						sb.setLength(0);
					} else {
						sb.append(c);// 拼接单词
					}
					i = stepForward(charsBefore, c, i);
				}
			}
		}
		setEmbedStartIndex(sqlMetaData, lineSplitorIndexs[0], lineSplitorIndexs[1]);
	}

	/**
	 * 向前前进一步
	 * 
	 * @param charsBefore
	 *            当前字符c前部的两个字符
	 * @param c
	 *            当前字符
	 * @param i
	 *            当前字符的位置
	 * @return 返回下一个索引值
	 */
	private static int stepForward(char[] charsBefore, char c, int i) {
		charsBefore[0] = charsBefore[1];
		charsBefore[1] = c;
		return ++i;
	}

	/**
	 * 确定SELECT子句之前最后一个换行符的位置
	 * 
	 * @param lineSplitorIndexs
	 *            SELECT子句之前最后一个换行符的位置
	 * @param c
	 *            当前字符
	 * @param i
	 *            当前字符的位置
	 */
	private static void decideLineSplitorIndex(int[] lineSplitorIndexs, char c, int i) {
		if (c == LINE_SEPARATOR[1]) {
			lineSplitorIndexs[1] = i;
		} else if (c == LINE_SEPARATOR[0]) {
			lineSplitorIndexs[0] = i;
		}
	}

	/**
	 * 设置查询嵌入的开始位置
	 * 
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 * @param r
	 *            /r的索引
	 * @param n
	 *            /n的索引
	 */
	private static void setEmbedStartIndex(SQLMetaData sqlMetaData, int r, int n) {
		int withIndex = sqlMetaData.getWithIndex(), selectIndex = sqlMetaData.getSelectIndex();
		if (withIndex < 0) {
			if (r < n) {
				if (n <= selectIndex) {
					sqlMetaData.setEmbedStartIndex(n + 1);
					return;
				}
			} else if (r > n) {
				if (r <= selectIndex) {
					sqlMetaData.setEmbedStartIndex(r + 1);
					return;
				}
			}
		}
		sqlMetaData.setEmbedStartIndex(selectIndex);
	}

	/**
	 * 设置查询嵌入的结束位置
	 * 
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 * @param r
	 *            /r的索引
	 * @param n
	 *            /n的索引
	 */
	private static void setEmbedEndIndex(SQLMetaData sqlMetaData, int r, int n) {
		int len = sqlMetaData.getLength();
		if (r < n) {
			if (r <= len) {
				sqlMetaData.setEmbedEndIndex(r);
				return;
			}
		} else if (r > n) {
			if (n <= len) {
				sqlMetaData.setEmbedEndIndex(n);
				return;
			}
		}
		sqlMetaData.setEmbedEndIndex(len);
	}

}
