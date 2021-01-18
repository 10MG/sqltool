package cn.tenmg.sqltool.dsql.filter;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.tenmg.sqltool.config.model.Filter;
import cn.tenmg.sqltool.config.model.filter.Eq;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.DateUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 参数等值过滤器
 * 
 * @author 赵伟均
 *
 */
public class EqParamFilter implements ParamFilter {

	/**
	 * 补位0
	 */
	private static final String ZERO = "0";

	/**
	 * 日期格式整理正则表达式
	 */
	private static final String DATE_REGEX = "(-| |/|:)";

	/**
	 * 日期对象格式化模板
	 */
	private static final String DATE_PATTERN = "yyyyMMddHHmmss";

	/**
	 * 日期对象格式化模板长度
	 */
	private static final int DATE_PATTERN_LENGTH = DATE_PATTERN.length();

	/**
	 * 时间格式整理正则表达式
	 */
	private static final String TIME_REGEX = ":";

	/**
	 * 时间对象格式化模板
	 */
	private static final String TIME_PATTERN = "HHmmss";

	/**
	 * 时间对象格式化模板长度
	 */
	private static final int TIME_PATTERN_LENGTH = TIME_PATTERN.length();

	// 将参数值等于指定值的参数过滤掉
	@Override
	public void doFilter(Filter filter, Map<String, ?> params) {
		List<Eq> eqs = filter.getEqs();
		if (CollectionUtils.isEmpty(eqs)) {
			return;
		}
		for (Iterator<Eq> it = eqs.iterator(); it.hasNext();) {
			Eq eq = it.next();
			String paramsConfig = eq.getParams();
			String value = eq.getValue();
			if (StringUtils.isNotBlank(value) && StringUtils.isNotBlank(paramsConfig)) {
				String paramNames[] = paramsConfig.split(",");
				for (int i = 0; i < paramNames.length; i++) {
					String paramName = paramNames[i].trim();
					if ("*".equals(paramName)) {
						doFilter(value, params);
						break;
					} else if (params.containsKey(paramName)) {
						doFilter(paramName, value, params);
					}
				}
			}
		}
	}

	/**
	 * 将所有参数值等于指定值的参数过滤掉
	 * 
	 * @param name
	 *            参数名
	 * @param value
	 *            比较的值
	 * @param params
	 *            参数集
	 */
	@SuppressWarnings("unchecked")
	private void doFilter(String value, Map<String, ?> params) {
		Iterator<?> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ?> entry = (Entry<String, ?>) it.next();
			Object paramValue = entry.getValue();
			if (paramValue != null && isFiltered(paramValue, value)) {
				it.remove();
			}
		}
	}

	/**
	 * 将指定参数名的参数值等于指定值的参数过滤掉
	 * 
	 * @param paramName
	 *            参数名
	 * @param value
	 *            比较的值
	 * @param params
	 *            参数集
	 */
	private void doFilter(String paramName, String value, Map<String, ?> params) {
		Object paramValue = params.get(paramName);
		if (paramValue != null && isFiltered(paramValue, value)) {
			params.remove(paramName);
		}
	}

	/**
	 * 将指定参数值等于指定值的参数过滤掉
	 * 
	 * @param paramValue
	 *            参数值
	 * @param value
	 *            比较的值
	 * @return
	 */
	private boolean isFiltered(Object paramValue, String value) {
		if (paramValue != null) {
			if (paramValue instanceof String) {
				if (value.equals((String) paramValue)) {
					return true;
				}
			} else if (paramValue instanceof Date || paramValue instanceof java.sql.Date
					|| paramValue instanceof Timestamp) {
				return isDateFiltered(paramValue, value);
			} else if (paramValue instanceof Time) {
				value = value.replaceAll(TIME_REGEX, "");// 格式整理
				if (StringUtils.isNumber(value)) {
					for (int j = value.length(); j < TIME_PATTERN_LENGTH; j++) {// 位数不足补0
						value = value.concat(ZERO);
					}
					if (value.equals(DateUtils.format(paramValue, DATE_PATTERN))) {
						return true;
					}
				}
			} else if (paramValue instanceof Calendar) {
				return isDateFiltered(paramValue, value);
			} else if (value.equals(paramValue.toString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判读日期参数是否可被过滤
	 * 
	 * @param paramValue
	 *            参数值
	 * @param value
	 *            比较的值
	 * 
	 * @return 参数可被过滤返回true，不可被过滤返回false
	 */
	private boolean isDateFiltered(Object paramValue, String value) {
		value = value.replaceAll(DATE_REGEX, "");// 格式整理
		if (StringUtils.isNumber(value)) {
			for (int j = value.length(); j < DATE_PATTERN_LENGTH; j++) {// 位数不足补0
				value = value.concat(ZERO);
			}
			if (value.equals(DateUtils.format(paramValue, DATE_PATTERN))) {
				return true;
			}
		}
		return false;
	}

}
