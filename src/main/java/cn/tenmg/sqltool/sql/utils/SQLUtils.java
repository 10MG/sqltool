package cn.tenmg.sqltool.sql.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.dsql.utils.DSQLUtils;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

public abstract class SQLUtils {

	private static final String WITH = "WITH", SELECT = "SELECT", FROM = "FROM", FROM_REVERSE = "MORF",
			ON_REVERSE = "NO", WHERE_REVERSE = "EREHW", GROUP_REVERSE = "PUORG", ORDER_REVERSE = "REDRO",
			BY_REVERSE = "YB", LIMIT_REVERSE = "TIMIL", OFFSET_REVERSE = "TESFFO", FETCH_REVERSE = "HCTEF",
			SELECT_SQL_TPL = "SELECT %s FROM %s%s", SPACE_WHERE_SPACE = " WHERE ";

	private static final int WITH_LEN = WITH.length(), SELECT_LEN = SELECT.length(), FROM_LEN = FROM.length();

	public static final char BLANK_SPACE = '\u0020', LEFT_BRACKET = '\u0028', RIGHT_BRACKET = '\u0029', COMMA = ',',
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
	 * 将指定的含有命名参数的源SQL及查询参数转换为JDBC可执行的SQL对象，该对象内含SQL脚本及对应的参数列表
	 * 
	 * @param source
	 *            源SQL脚本
	 * @param params
	 *            查询参数列表
	 * @return 返回JDBC可执行的SQL对象，含SQL脚本及对应的参数列表
	 */
	public static SQL toSQL(String source, Map<String, ?> params) {
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		List<Object> paramList = new ArrayList<Object>();
		if (StringUtils.isBlank(source)) {
			return new SQL(source, paramList);
		}
		int len = source.length(), i = 0;
		char a = ' ', b = ' ';
		boolean isString = false;// 是否在字符串区域
		boolean isParam = false;// 是否在参数区域
		StringBuilder sql = new StringBuilder(), paramName = new StringBuilder();
		while (i < len) {
			char c = source.charAt(i);
			if (isString) {
				if (isStringEnd(a, b, c)) {// 字符串区域结束
					isString = false;
				}
				sql.append(c);
			} else {
				if (c == SINGLE_QUOTATION_MARK) {// 字符串区域开始
					isString = true;
					sql.append(c);
				} else if (isParam) {// 处于参数区域
					if (DSQLUtils.isParamChar(c)) {
						paramName.append(c);
					} else {
						isParam = false;// 参数区域结束
						paramEnd(params, sql, paramName, paramList);
						sql.append(c);
					}
				} else {
					if (DSQLUtils.isParamBegin(b, c)) {
						isParam = true;// 参数区域开始
						paramName.setLength(0);
						paramName.append(c);
						sql.setCharAt(sql.length() - 1, '?');// “:”替换为“?”
					} else {
						sql.append(c);
					}
				}
			}
			a = b;
			b = c;
			i++;
		}
		if (isParam) {
			paramEnd(params, sql, paramName, paramList);
		}
		return new SQL(sql.toString(), paramList);
	}

	/**
	 * 获取SQL相关数据（不对SQL做null校验）
	 * 
	 * @param sql
	 *            SQL
	 * @return 返回SQL相关数据对象
	 */
	public static SQLMetaData getSQLMetaData(String sql) {
		SQLMetaData sqlMetaData = new SQLMetaData();
		sqlMetaData.setLength(sql.length());
		rightAnalysis(sql, sqlMetaData);
		leftAnalysis(sql, sqlMetaData);
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

	private static final String SELECT_ALL = "SELECT * FROM (\n", ALIAS = "\n) SQLTOOL",
			WHERE_IMPOSSIBLE = "\nWHERE 1=0";

	/**
	 * 获取SQL字段名列表
	 * 
	 * @param con
	 *            已打开的数据库连接
	 * @param sql
	 *            SQL
	 * @param params
	 *            查询参数集
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 * @return 返回SQL字段名列表
	 * @throws SQLException
	 *             SQL异常
	 */
	public static final String[] getColumnLabels(Connection con, String sql, Map<String, ?> params,
			SQLMetaData sqlMetaData) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String script, columnLabels[] = null;
		try {
			int length = sqlMetaData.getLength(), embedStartIndex = sqlMetaData.getEmbedStartIndex(),
					embedEndIndex = sqlMetaData.getEmbedEndIndex();
			if (embedStartIndex > 0) {
				if (embedEndIndex < length) {
					script = sql.substring(0, embedStartIndex).concat(SELECT_ALL)
							.concat(sql.substring(embedStartIndex, embedEndIndex)).concat(ALIAS)
							.concat(WHERE_IMPOSSIBLE).concat(sql.substring(embedEndIndex));
				} else {
					script = sql.substring(0, embedStartIndex).concat(SELECT_ALL).concat(sql.substring(embedStartIndex))
							.concat(ALIAS).concat(WHERE_IMPOSSIBLE);
				}
			} else {
				if (embedEndIndex < length) {
					script = SELECT_ALL.concat(sql.substring(0, embedEndIndex)).concat(ALIAS).concat(WHERE_IMPOSSIBLE)
							.concat(sql.substring(embedEndIndex));
				} else {
					script = SELECT_ALL.concat(sql).concat(ALIAS).concat(WHERE_IMPOSSIBLE);
				}
			}
			SQL SQL = toSQL(script, params);
			ps = con.prepareStatement(SQL.getScript());
			JdbcUtils.setParams(ps, SQL.getParams());
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			columnLabels = new String[columnCount];
			for (int i = 1; i <= columnCount; i++) {
				columnLabels[i - 1] = rsmd.getColumnLabel(i);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(ps);
		}
		return columnLabels;
	}

	private static void paramEnd(Map<String, ?> params, StringBuilder sql, StringBuilder paramName,
			List<Object> paramList) {
		String name = paramName.toString();
		Object value = params.get(name);
		if (value != null) {
			if (value instanceof Collection<?>) {
				Collection<?> collection = (Collection<?>) value;
				if (CollectionUtils.isEmpty(collection)) {
					paramList.add(null);
				} else {
					boolean flag = false;
					for (Iterator<?> it = collection.iterator(); it.hasNext();) {
						if (flag) {
							sql.append(", ?");
						} else {
							flag = true;
						}
						paramList.add(it.next());
					}
				}
			} else if (value instanceof Object[]) {
				Object[] objects = (Object[]) value;
				if (objects.length == 0) {
					paramList.add(null);
				} else {
					for (int j = 0; j < objects.length; j++) {
						if (j > 0) {
							sql.append(", ?");
						}
						paramList.add(objects[j]);
					}
				}
			} else {
				paramList.add(value);
			}
		} else {
			paramList.add(value);
		}
	}

	/**
	 * 分析SQL右边部分
	 * 
	 * @param sql
	 *            SQL
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 */
	private static void rightAnalysis(String sql, SQLMetaData sqlMetaData) {
		int length = sqlMetaData.getLength(), i = length - 1;
		char c = sql.charAt(i);
		boolean isString = false;
		int deep = 0, lineSplitorIndexs[] = { length, length };
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
							if (BY_REVERSE.equalsIgnoreCase(sa)) {
								if (GROUP_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setGroupByIndex(i + 1);
									break;
								} else if (ORDER_REVERSE.equalsIgnoreCase(sb)) {
									sqlMetaData.setOrderByIndex(i + 1);
								}
							} else if (LIMIT_REVERSE.equalsIgnoreCase(sb)) {
								sqlMetaData.setLimitIndex(i + 1);
							} else if (FETCH_REVERSE.equalsIgnoreCase(sb)) {
								sqlMetaData.setFetchIndex(i + 1);
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
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 */
	private static void leftAnalysis(String sql, SQLMetaData sqlMetaData) {
		int i = 0, deep = 0, max = sqlMetaData.getLength(), fromIndex = sqlMetaData.getFromIndex(),
				whereIndex = sqlMetaData.getWhereIndex();
		if (whereIndex > 0) {// 含有WHERE子句，只需扫描到WHERE之前即可
			max = whereIndex;
		} else if (fromIndex > 0) {// 没有WHERE子句，但有FROM子句，只需扫描到FROM之前即可
			max = fromIndex;
		}
		int[] lineSplitorIndexs = { 0, 0 };
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
							sb.setLength(0);
						} else if (c == RIGHT_BRACKET) {// 右括号
							deep--;
							sb.setLength(0);
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
						sb.setLength(0);
					} else if (c == RIGHT_BRACKET) {// 右括号
						deep--;
						sb.setLength(0);
					} else if (c <= BLANK_SPACE) {// 遇到空白字符
						if (deep == 0) {
							if (sqlMetaData.getSelectIndex() < 0) {
								decideLineSplitorIndex(lineSplitorIndexs, c, i);
							}
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
		if (selectIndex > 0) {
			if (withIndex >= 0 && selectIndex > withIndex) {
				sqlMetaData.setEmbedStartIndex(selectIndex);
			} else {
				if (r < n && n < selectIndex) {
					sqlMetaData.setEmbedStartIndex(n + 1);
				} else if (r > n && r < selectIndex) {
					sqlMetaData.setEmbedStartIndex(r + 1);
				} else {
					sqlMetaData.setEmbedStartIndex(selectIndex);
				}
			}
		} else {
			sqlMetaData.setEmbedStartIndex(0);
		}
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
		if (r < n) {
			sqlMetaData.setEmbedEndIndex(r);
		} else if (r > n) {
			sqlMetaData.setEmbedEndIndex(n);
		} else {
			sqlMetaData.setEmbedEndIndex(sqlMetaData.getLength());
		}
	}

}
