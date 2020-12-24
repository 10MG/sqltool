package cn.tenmg.sqltool.sql.engine;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.tenmg.sqltool.dsql.Sql;
import cn.tenmg.sqltool.dsql.utils.DsqlUtils;
import cn.tenmg.sqltool.sql.SqlEngine;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 抽象SQL引擎
 * 
 * @author 赵伟均
 *
 */
public abstract class AbstractSqlEngine implements SqlEngine {

	/**
	 * 
	 */
	private static final long serialVersionUID = -867998927447365357L;

	private static final char SINGLE_QUOTATION_MARK = '\'';

	abstract String parse(Date date);

	abstract String parse(Calendar calendar);

	@Override
	public String parse(Sql sql) {
		String source = sql.getScript();
		Map<String, Object> params = sql.getParams();
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		if (StringUtils.isBlank(source)) {
			return source;
		}
		int len = source.length(), i = 0;
		char a = ' ', b = ' ';
		boolean isString = false;// 是否在字符串区域
		boolean isParam = false;// 是否在参数区域
		StringBuilder sb = new StringBuilder(), paramName = new StringBuilder();
		while (i < len) {
			char c = source.charAt(i);
			if (isString) {
				if (DsqlUtils.isStringEnd(a, b, c)) {// 字符串区域结束
					isString = false;
				}
				sb.append(c);
			} else {
				if (c == SINGLE_QUOTATION_MARK) {// 字符串区域开始
					isString = true;
					sb.append(c);
				} else if (isParam) {// 处于参数区域
					if (DsqlUtils.isParamChar(c)) {
						paramName.append(c);
					} else {
						isParam = false;// 参数区域结束
						String name = paramName.toString();
						parseParam(sb, name, params.get(name));
						sb.append(c);
					}
				} else {
					if (DsqlUtils.isParamBegin(b, c)) {
						isParam = true;// 参数区域开始
						paramName.setLength(0);
						paramName.append(c);
						sb.setLength(sb.length() - 1);// 去掉 “:”
					} else {
						sb.append(c);
					}
				}
			}
			a = b;
			b = c;
			i++;
		}
		if (isParam) {
			String name = paramName.toString();
			parseParam(sb, name, params.get(name));
		}
		return sb.toString();
	}

	private void parseParam(StringBuilder sb, String name, Object value) {
		if (value == null) {
			appendNull(sb);
		} else {
			if (value instanceof Collection<?>) {
				Collection<?> collection = (Collection<?>) value;
				if (CollectionUtils.isEmpty(collection)) {
					appendNull(sb);
				} else {
					boolean flag = false;
					for (Iterator<?> it = collection.iterator(); it.hasNext();) {
						if (flag) {
							sb.append(JdbcUtils.COMMA_SPACE);
						} else {
							flag = true;
						}
						append(sb, it.next());
					}
				}
			} else if (value instanceof Object[]) {
				Object[] objects = (Object[]) value;
				if (objects.length == 0) {
					appendNull(sb);
				} else {
					for (int j = 0; j < objects.length; j++) {
						if (j > 0) {
							sb.append(JdbcUtils.COMMA_SPACE);
						}
						append(sb, objects[j]);
					}
				}
			} else {
				append(sb, value);
			}
		}
	}

	private void append(StringBuilder sb, Object value) {
		if (value instanceof String || value instanceof char[]) {
			appendString(sb, (String) value);
		} else if (value instanceof Date) {
			appendString(sb, parse((Date) value));
		} else if (value instanceof Calendar) {
			appendString(sb, parse((Calendar) value));
		} else {
			sb.append(value.toString());
		}
	}

	private static final void appendNull(StringBuilder sb) {
		sb.append("NULL");
	}

	private static final void appendString(StringBuilder sb, String value) {
		sb.append("'").append(value).append("'");
	}

}
