package cn.tenmg.sqltool.utils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class PlaceHolderUtils {

	private static final Pattern paramPattern = Pattern.compile("\\$\\{\\w+\\}"),
			arrayPattern = Pattern.compile("\\[[^\\]]+\\]");

	/**
	 * 将模板字符串中占位符替换为指定的参数
	 * 
	 * @param tpl
	 *            模板字符串
	 * @param params
	 *            参数集（分别列出参数名和参数值）
	 * @return 返回将模板字符串中占位符替换为指定的参数后的字符串
	 */
	public static String replace(String tpl, CharSequence... params) {
		if (params != null && params.length > 1) {
			Map<CharSequence, CharSequence> map = new HashMap<CharSequence, CharSequence>();
			for (int i = 0; i < params.length; i += 2) {
				map.put(params[i], params[i + 1]);
			}
			return replace(tpl, map);
		} else {
			return tpl;
		}
	}

	/**
	 * 将模板字符串中占位符替换为指定的参数
	 * 
	 * @param tpl
	 *            模板字符串
	 * @param params
	 *            参数集
	 * @return 返回将模板字符串中占位符替换为指定的参数后的字符串
	 */
	public static String replace(String tpl, Map<? extends CharSequence, ? extends CharSequence> params) {
		if (!CollectionUtils.isEmpty(params)) {
			StringBuffer sb = new StringBuffer();
			Matcher m = paramPattern.matcher(tpl);
			String name;
			Object value;
			while (m.find()) {
				name = m.group();
				value = getParam(params, name.substring(2, name.length() - 1));
				if (value != null) {
					m.appendReplacement(sb, value.toString());
				} else {
					m.appendReplacement(sb, "");
				}
			}
			m.appendTail(sb);
			return sb.toString();
		} else {
			return tpl;
		}
	}

	/**
	 * 获取Map参数集中的参数值
	 * 
	 * @param params
	 *            参数集
	 * @param name
	 *            参数名称。可以是paramName也可以是paramName.fieldName的形式
	 * @return 参数不存在则返回null，否则返回参数值
	 */
	public static Object getParam(Map<? extends CharSequence, ?> params, String name) {
		Object value = params.get(name);
		if (value == null) {
			if (name.contains(".")) {
				String[] names = name.split("\\.");
				name = names[0];
				value = params.get(name);
				if (value == null) {
					return getArrayValue(params, name);
				} else {
					for (int i = 1; i < names.length; i++) {
						name = names[i];
						value = ObjectUtils.getValue(value, name);
						if (value == null) {
							Matcher m = arrayPattern.matcher(name);
							if (m.find()) {
								value = ObjectUtils.getValue(value, name.substring(0, name.indexOf("[")));
								if (value == null) {
									return null;
								} else {
									value = getGroupValue(value, params, m);
								}
							}
							return value;
						}
					}
					return value;
				}
			} else {
				return getArrayValue(params, name);
			}
		} else {
			return value;
		}
	}

	private static final Object getArrayValue(Map<? extends CharSequence, ?> params, String name) {
		Object value = null;
		Matcher m = arrayPattern.matcher(name);
		if (m.find()) {
			value = params.get(name.substring(0, name.indexOf("[")));
			if (value == null) {
				return null;
			} else {
				value = getGroupValue(value, params, m);
			}
		}
		return value;
	}

	private static final Object getGroupValue(Object value, Map<? extends CharSequence, ?> params, Matcher m) {
		value = getGroupValue(value, params, m.group());
		while (value != null && m.find()) {
			value = getGroupValue(value, params, m.group());
		}
		return value;
	}

	private static final Object getGroupValue(Object value, Map<? extends CharSequence, ?> params, String group) {
		String name = group.substring(1, group.length() - 1);
		return getValue(value, params, name);
	}

	private static final Object getValue(Object value, Map<? extends CharSequence, ?> params, String name) {
		Object v = params.get(name);
		String real = name;
		if (v != null) {
			real = v.toString();
		}
		if (value instanceof Map) {
			return ((Map<?, ?>) value).get(real);
		} else if (value instanceof List) {
			return ((List<?>) value).get(Integer.valueOf(real));
		} else if (value instanceof Object[]) {
			return ((Object[]) value)[Integer.valueOf(real)];
		} else if (value instanceof LinkedHashSet) {
			return ((LinkedHashSet<?>) value).toArray()[Integer.valueOf(real)];
		} else {
			return null;
		}
	}

}
