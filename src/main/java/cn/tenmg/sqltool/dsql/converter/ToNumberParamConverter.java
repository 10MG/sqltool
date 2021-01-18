package cn.tenmg.sqltool.dsql.converter;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.model.Converter;
import cn.tenmg.sqltool.config.model.converter.ToNumber;
import cn.tenmg.sqltool.exception.ConvertException;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.DecimalUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 参数数字类型转换器
 * 
 * @author 赵伟均
 *
 */
public class ToNumberParamConverter implements ParamConverter {

	/**
	 * 将参数转换为数字类型
	 */
	@Override
	public void convert(Converter converter, Map<String, Object> params) {
		List<ToNumber> toNumbers = converter.getToNumbers();
		if (CollectionUtils.isEmpty(toNumbers)) {
			return;
		}
		for (Iterator<ToNumber> it = toNumbers.iterator(); it.hasNext();) {
			ToNumber toNumber = it.next();
			String paramsConfig = toNumber.getParams();
			String formatter = toNumber.getFormatter();
			if (StringUtils.isNotBlank(paramsConfig)) {
				String paramsNames[] = paramsConfig.split(",");
				for (int i = 0; i < paramsNames.length; i++) {
					String paramName = paramsNames[i].trim();
					if ("*".equals(paramName)) {
						Set<String> set = params.keySet();
						for (Iterator<String> nit = set.iterator(); nit.hasNext();) {
							paramName = nit.next();
							Object v = params.get(paramName);
							Number number = null;
							if (v != null) {
								try {
									number = DecimalUtils.parse(v, formatter);
								} catch (ParseException e) {
									e.printStackTrace();
									final String msg = String.format("将参数%s：%s，按模板：%s，转换为数字对象失败", paramName,
											v.toString(), formatter);
									throw new ConvertException(msg, e);
								}
							}
							params.put(paramName, number);
						}
						break;
					} else if (params.containsKey(paramName)) {
						Object v = params.get(paramName);
						Number number = null;
						if (v != null) {
							try {
								number = DecimalUtils.parse(v, formatter);
							} catch (ParseException e) {
								e.printStackTrace();
								final String msg = String.format("将参数%s：%s，按模板：%s，转换为数字对象失败", paramName, v.toString(),
										formatter);
								throw new ConvertException(msg, e);
							}
						}
						params.put(paramName, number);
					}
				}
			}
		}
	}

}
