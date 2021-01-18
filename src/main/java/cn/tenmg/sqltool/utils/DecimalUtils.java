package cn.tenmg.sqltool.utils;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * 实数工具类
 * 
 * @author 赵伟均
 *
 */
public abstract class DecimalUtils {
	/**
	 * 根据模板将指定对象格式化数字字符串
	 * 
	 * @param obj
	 *            指定对象
	 * @param pattern
	 *            模板
	 * @return 数字字符串
	 */
	public static String format(Object obj, String pattern) {
		DecimalFormat df = new DecimalFormat(pattern);
		return df.format(obj);
	}

	/**
	 * 根据模板将指定对象转换为数字对象
	 * 
	 * @param obj
	 *            指定对象
	 * @param pattern
	 *            模板
	 * @return 数字对象
	 * @throws ParseException
	 *             如果无法将对象转换，将抛出此异常
	 */

	public static Number parse(Object obj, String pattern) throws ParseException {
		DecimalFormat df = new DecimalFormat(pattern);
		if (obj instanceof String) {
			return df.parse((String) obj);
		}
		return df.parse(df.format(obj));
	}
}