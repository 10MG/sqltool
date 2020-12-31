package cn.tenmg.sqltool.dsql.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 动态结构化查询语言(DSQL)工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class DSQLUtils {

	private static final char BLANK_SPACE = '\u0020';

	private static final char PARAM_BEGIN = ':';

	/**
	 * 将指定的源动态结构化查询语言（DSQL）及查询参数转换为含有命名参数的SQL（命名参数以“:”开头）以及实际使用的参数集组成的对象
	 * 
	 * @param source
	 *            源DSQL脚本
	 * @param params
	 *            查询参数列表
	 * @return 返回含有命名参数的SQL以及实际使用的参数集组成的对象
	 */
	public static NamedSQL parse(String source, Map<String, Object> params) {
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		if (StringUtils.isBlank(source)) {
			return new NamedSQL(source, params);
		}
		int len = source.length();
		if (len < 3) {// 长度小于最小动态SQL单元 #[]的长度直接返回
			return new NamedSQL(source, params);
		}
		int i = 0, deep = 0;
		char a = ' ', b = ' ';
		boolean isString = false;// 是否在字符串区域
		boolean isDynamic = false;// 是否在动态SQL区域
		boolean isParam = false;// 是否在参数区域
		StringBuilder sql = new StringBuilder(), paramName = new StringBuilder();
		Map<String, Object> usedParams = new HashMap<String, Object>();
		HashMap<Integer, Boolean> inValidMap = new HashMap<Integer, Boolean>();
		HashMap<Integer, Set<String>> validMap = new HashMap<Integer, Set<String>>();
		HashMap<Integer, StringBuilder> dsqlMap = new HashMap<Integer, StringBuilder>();
		HashMap<Integer, Map<String, Object>> contexts = new HashMap<Integer, Map<String, Object>>();
		while (i < len) {
			char c = source.charAt(i);
			if (isString) {
				if (isStringEnd(a, b, c)) {// 字符串区域结束
					isString = false;
				}
				if (deep > 0) {
					dsqlMap.get(deep).append(c);
				} else {
					sql.append(c);
				}
			} else {
				if (c == JdbcUtils.SINGLE_QUOTATION_MARK) {// 字符串区域开始
					isString = true;
					if (deep > 0) {
						dsqlMap.get(deep).append(c);
					} else {
						sql.append(c);
					}
				} else if (isDynamic) {// 当前字符处于动态SQL区域
					if (isDynamicEnd(c)) {// 结束当前动态SQL区域
						if (isParam) {// 处于动态参数区域
							isParam = false;// 结束动态参数区域
							String name = paramName.toString();
							Object value = params.get(name);
							if (value != null) {
								validMap.get(deep).add(name);
								paramName.setLength(0);
							} else if (deep > 0) {
								inValidMap.put(deep, Boolean.TRUE);// 含有无效参数标记
							}
						}
						if (inValidMap.get(deep) == null) {// 不含无效参数
							processDsql(params, sql, dsqlMap, usedParams, inValidMap, validMap, contexts, deep, false);
							deep--;
						} else if (deep > 0) {
							processDsql(params, sql, dsqlMap, usedParams, inValidMap, validMap, contexts, deep, true);
							deep--;
						}
						if (deep < 1) {// 已离开动态SQL区域
							isDynamic = false;
							deleteRedundantBlank(sql);// 删除多余空白字符
						} else {
							deleteRedundantBlank(dsqlMap.get(deep));// 删除多余空白字符
						}
					} else {
						if (isParam) {// 处于动态参数区域
							if (DSQLUtils.isParamChar(c)) {
								paramName.append(c);

								StringBuilder dsql = dsqlMap.get(deep);
								if (dsql == null) {
									dsql = new StringBuilder();
									dsqlMap.put(deep, dsql);
								}
								dsql.append(c);
							} else {// 离开动态参数区域
								isParam = false;
								String name = paramName.toString();
								Object value = params.get(name);
								if (value != null) {
									validMap.get(deep).add(name);
								} else if (deep >= 0) {
									inValidMap.put(deep, Boolean.TRUE);// 含有无效参数标记
								}
								paramName.setLength(0);

								if (isDynamicBegin(b, c)) {// 嵌套的新的动态SQL区域
									StringBuilder dsql = dsqlMap.get(deep);
									dsql.deleteCharAt(dsql.length() - 1);// 删除#号
									deep++;
									dsqlMap.put(deep, new StringBuilder());
									validMap.put(deep, new HashSet<String>());
								} else {
									StringBuilder dsql = dsqlMap.get(deep);
									if (dsql == null) {
										dsql = new StringBuilder();
										dsqlMap.put(deep, dsql);
									}
									dsql.append(c);
								}
							}
						} else {// 未处于动态参数区域
							if (isDynamicBegin(b, c)) {// 嵌套的新的动态SQL区域
								StringBuilder dsql = dsqlMap.get(deep);
								dsql.deleteCharAt(dsql.length() - 1);// 删除#号
								deep++;
								dsqlMap.put(deep, new StringBuilder());
								validMap.put(deep, new HashSet<String>());
							} else {
								if (DSQLUtils.isParamBegin(b, c)) {
									isParam = true;
									paramName.setLength(0);
									paramName.append(c);
								}

								StringBuilder dsql = dsqlMap.get(deep);
								if (dsql == null) {
									dsql = new StringBuilder();
									dsqlMap.put(deep, dsql);
								}
								dsql.append(c);
							}
						}
					}
				} else {// 当前字符未处于动态SQL区域
					if (isDynamicBegin(b, c)) {
						isDynamic = true;
						sql.deleteCharAt(sql.length() - 1);
						deep++;
						validMap.put(deep, new HashSet<String>());
					} else {
						if (isParam) {// 处于参数区域
							if (DSQLUtils.isParamChar(c)) {
								paramName.append(c);
								if (i == len - 1) {
									String name = paramName.toString();
									usedParams.put(name, params.get(name));
								}
							} else {// 离开参数区域
								isParam = false;
								String name = paramName.toString();
								usedParams.put(name, params.get(name));
								if (i < len - 1) {
									paramName.setLength(0);
								}
							}
						} else {// 未处于参数区域
							if (DSQLUtils.isParamBegin(b, c)) {
								isParam = true;
								paramName.setLength(0);
								paramName.append(c);
							}
						}
						sql.append(c);
					}
				}
			}
			a = b;
			b = c;
			i++;
		}
		return new NamedSQL(sql.toString(), usedParams);
	}

	/**
	 * 将指定的源动态结构化查询语言（DSQL）及查询参数转换为含有命名参数的SQL（命名参数以“:”开头）以及实际使用的参数集组成的对象
	 * 
	 * @param source
	 *            源DSQL脚本
	 * @param params
	 *            查询参数列表
	 * @return 返回含有命名参数的SQL以及实际使用的参数集组成的对象
	 */
	@SuppressWarnings("unchecked")
	public static NamedSQL parse(String source, Object... params) {
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		if (params != null) {
			if (params.length == 1 && params[0] instanceof Map) {
				paramsMap = (Map<String, Object>) params[0];
			} else {
				for (int i = 0; i < params.length; i++) {
					paramsMap.put((String) params[i], params[++i]);
				}
			}
		}
		return parse(source, paramsMap);
	}

	/**
	 * 将指定的（含有命名参数的）源SQL及查询参数转换为JDBC可执行的对象（内含SQL及对应的参数列表）
	 * 
	 * @param source
	 *            源SQL脚本
	 * @param params
	 *            查询参数列表
	 * @return 返回JDBC可执行的SQL对象（含脚本及对应参数）
	 */
	public static SQL toSQL(String source, Map<String, Object> params) {
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
				if (c == JdbcUtils.SINGLE_QUOTATION_MARK) {// 字符串区域开始
					isString = true;
					sql.append(c);
				} else if (isParam) {// 处于参数区域
					if (isParamChar(c)) {
						paramName.append(c);
					} else {
						isParam = false;// 参数区域结束
						paramEnd(params, sql, paramName, paramList);
						sql.append(c);
					}
				} else {
					if (isParamBegin(b, c)) {
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
	 * 根据指定的两个前后相邻字符b和c，判断其是否为SQL参数的开始位置
	 * 
	 * @param b
	 *            前一个字符
	 * @param c
	 *            当前字符
	 * @return 如果字符b为“:”且字符c为26个英文字母（大小写均可）则返回true，否则返回false
	 */
	public static boolean isParamBegin(char b, char c) {
		return b == PARAM_BEGIN && is26LettersIgnoreCase(c);
	}

	/**
	 * 根据指定的字符c，判断是否是参数字符（即大小写字母、数字、下划线、短横线）
	 * 
	 * @param c
	 *            指定字符
	 * @return 如果字符c为26个字母（大小写均可）、0-9数字、_或者-，返回true，否则返回false
	 */
	public static boolean isParamChar(char c) {
		return is26LettersIgnoreCase(c) || (c >= '0' && c <= '9') || c == '_' || c == '-';
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
		return (a == JdbcUtils.SINGLE_QUOTATION_MARK
				|| (a != JdbcUtils.SINGLE_QUOTATION_MARK && b != JdbcUtils.SINGLE_QUOTATION_MARK))
				&& c == JdbcUtils.SINGLE_QUOTATION_MARK;
	}

	/**
	 * 处理DSQL片段
	 * 
	 * @param params
	 *            查询参数
	 * @param sql
	 *            目标SQL字符串构建器
	 * @param dsqlMap
	 *            DSQL片段（带深度）缓存表
	 * @param usedParams
	 *            使用到的参数
	 * @param inValidMap
	 *            含有无效参数标记
	 * @param validMap
	 *            有效参数表
	 * @param contexts
	 *            宏运行上下文
	 * @param deep
	 *            当前动态SQL深度
	 * @param emptyWhenNoMacro
	 *            DSQL片段没有宏时目标SQL为空白字符串
	 */
	private static final void processDsql(Map<String, Object> params, StringBuilder sql,
			HashMap<Integer, StringBuilder> dsqlMap, Map<String, Object> usedParams,
			HashMap<Integer, Boolean> inValidMap, HashMap<Integer, Set<String>> validMap,
			HashMap<Integer, Map<String, Object>> contexts, int deep, boolean emptyWhenNoMacro) {
		Map<String, Object> context = contexts.get(deep);
		if (context == null) {
			context = new HashMap<String, Object>();
			contexts.put(deep, context);
		}
		StringBuilder dsql = MacroUtils.execute(dsqlMap.get(deep), context, params, emptyWhenNoMacro);
		if (deep == 1) {
			sql.append(dsql);
			for (String name : validMap.get(deep)) {
				if (!usedParams.containsKey(name) && dsql.indexOf(PARAM_BEGIN + name) >= 0) {
					usedParams.put(name, params.get(name));
				}
			}
		} else {
			dsqlMap.get(deep - 1).append(dsql);
		}
		dsqlMap.remove(deep);
		validMap.remove(deep);
		inValidMap.remove(deep);
	}

	/**
	 * 根据指定的两个前后相邻字符b和c，判断其是否为动态SQL区的开始位置
	 * 
	 * @param b
	 *            前一个字符b
	 * @param c
	 *            当前字符c
	 * @return
	 */
	private static boolean isDynamicBegin(char b, char c) {
		return b == '#' && c == '[';
	}

	/**
	 * 根据指定字符c，判断其是否为动态SQL区的结束位置
	 * 
	 * @param c
	 *            指定字符
	 * @return
	 */
	private static boolean isDynamicEnd(char c) {
		return c == ']';
	}

	/**
	 * 删除将指定字符串缓冲区末尾多余的空白字符（包括回车符、换行符、制表符、空格）
	 * 
	 * @param target
	 *            指定字符串缓冲区
	 */
	private static void deleteRedundantBlank(StringBuilder target) {
		int length = target.length(), i = length;
		while (i > 0) {
			char ch = target.charAt(--i);
			if (ch > BLANK_SPACE) {
				target.delete(i + 1, length);
				break;
			}
		}
	}

	private static void paramEnd(Map<String, Object> params, StringBuilder sql, StringBuilder paramName,
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
	 * 根据指定的字符c，判断是否是26个字母（大小写均可）
	 * 
	 * @param c
	 *            指定字符
	 * @return 是26个字母返回true，否则返回false
	 */
	private static boolean is26LettersIgnoreCase(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

}
