package cn.tenmg.sqltool.dsql.converter;

import java.util.Map;

import cn.tenmg.sqltool.config.model.Converter;

/**
 * 参数转换器
 * 
 * @author 赵伟均
 *
 */
public interface ParamConverter {
	/**
	 * 参数转换处理程序
	 * 
	 * @param converter
	 *            参数转换器配置对象
	 * @param params
	 *            参数集
	 */
	void convert(Converter converter, Map<String, Object> params);
}
