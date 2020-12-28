package cn.tenmg.sqltool.dsql.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import javax.script.ScriptException;

import cn.tenmg.sqltool.dsql.macro.Macro;

/**
 * 宏工具类
 * 
 * @author 赵伟均
 *
 */
public abstract class MacroUtils {

	private static final char BLANK_SPACE = '\u0020';

	/**
	 * 宏逻辑开始
	 */
	private static final char MACRO_LOGIC_START = '(';

	/**
	 * 宏逻辑结束
	 */
	private static final char MACRO_LOGIC_END = ')';

	/**
	 * 宏
	 */
	private static final Map<String, Macro> MACROS = new HashMap<String, Macro>();

	static {
		ServiceLoader<Macro> loader = ServiceLoader.load(Macro.class);
		for (Iterator<Macro> it = loader.iterator(); it.hasNext();) {
			Macro macro = it.next();
			MACROS.put(macro.getClass().getSimpleName().toLowerCase(), macro);
		}
	}

	public static final StringBuilder execute(StringBuilder dsql, Map<String, Object> context,
			Map<String, Object> params, boolean returnEmptyWhenNoMacro) {
		int len = dsql.length(), i = 0;
		char a = ' ', b = ' ';
		StringBuilder macroName = new StringBuilder(), paramName = null;
		Map<String, Object> usedParams = new HashMap<String, Object>();
		while (i < len) {
			char c = dsql.charAt(i);
			if (c == MACRO_LOGIC_START) {// 宏逻辑开始
				if (macroName.length() > 0) {
					Macro macro = MACROS.get(macroName.toString());
					if (macro == null) {// 找不到对应的宏
						if (returnEmptyWhenNoMacro) {
							return new StringBuilder();
						}
						return dsql;
					} else {
						StringBuilder logic = new StringBuilder();
						boolean isString = false;// 是否在字符串区域
						boolean isParam = false;// 是否在参数区域
						int deep = 0;// 逻辑深度
						while (++i < len) {
							a = b;
							b = c;
							c = dsql.charAt(i);
							if (isString) {
								if (DSQLUtils.isStringEnd(a, b, c)) {// 字符串区域结束
									isString = false;
								}
								logic.append(c);
							} else {
								if (c == MACRO_LOGIC_END) {// 宏逻辑结束
									if (deep == 0) {
										if (logic.length() > 0) {
											return execute(dsql, context, usedParams, macro, logic.toString(), i);// 当前字符为括号，当前位置即为宏名称及逻辑结束的位置
										} else {
											return dsql;
										}
									} else {
										logic.append(c);
										deep--;
									}
								} else if (c == MACRO_LOGIC_START) {
									logic.append(c);
									deep++;
								} else {
									if (isParam) {
										if (DSQLUtils.isParamChar(c)) {
											paramName.append(c);
										} else {
											isParam = false;
											String name = paramName.toString();
											usedParams.put(name, params.get(name));
										}
										logic.append(c);
									} else {
										if (DSQLUtils.isParamBegin(b, c)) {
											isParam = true;
											paramName = new StringBuilder();
											paramName.append(c);
											logic.setCharAt(logic.length() - 1, c);
										} else {
											logic.append(c);
										}
									}
								}
							}
						}
					}
					return dsql;
				} else {
					if (returnEmptyWhenNoMacro) {
						return new StringBuilder();
					}
					return dsql;
				}
			} else if (c <= BLANK_SPACE) {
				if (macroName.length() > 0) {
					Macro macro = MACROS.get(macroName.toString());
					if (macro == null) {// 找不到对应的宏
						if (returnEmptyWhenNoMacro) {
							return new StringBuilder();
						}
						return dsql;
					} else {
						return execute(dsql, context, usedParams, macro, null, i - 1);// 当前字符为空白字符，则宏名称结束应该在前一个位置
					}
				} else {
					if (returnEmptyWhenNoMacro) {
						return new StringBuilder();
					}
					return dsql;
				}
			} else {
				macroName.append(c);
			}
			a = b;
			b = c;
			i++;
		}
		return dsql;
	}

	private static final StringBuilder execute(StringBuilder dsql, Map<String, Object> context,
			Map<String, Object> params, Macro macro, String logic, int macroEndIndex) {
		Object result = null;
		try {
			result = macro.excute(logic, context, params);
		} catch (ScriptException e) {
			return dsql;
		}
		if (result == null) {
			return dsql;
		} else {
			if (result instanceof Boolean) {
				if (((Boolean) result).booleanValue()) {
					dsql.delete(0, macroEndIndex + 1);
					return dsql;
				} else {
					return new StringBuilder();
				}
			} else {
				return new StringBuilder(result.toString()).append(dsql.delete(0, macroEndIndex + 1));
			}
		}
	}
}
