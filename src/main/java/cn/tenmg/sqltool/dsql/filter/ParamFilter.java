package cn.tenmg.sqltool.dsql.filter;

import java.util.Map;

import cn.tenmg.sqltool.config.model.Filter;

/**
 * 参数过滤器
 * 
 * @author 赵伟均
 *
 */
public interface ParamFilter {
	/**
	 * 参数过滤处理程序
	 * 
	 * @param filter
	 *            参数过滤器配置对象
	 * @param params
	 *            参数集
	 */
	void doFilter(Filter filter, Map<String, ?> params);
}