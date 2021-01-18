package cn.tenmg.sqltool.dsql.converter;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.model.Converter;
import cn.tenmg.sqltool.config.model.converter.ToDate;
import cn.tenmg.sqltool.exception.ConvertException;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.DateUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 参数日期类型转换器
 * 
 * @author 赵伟均
 *
 */
public class ToDateParamConverter implements ParamConverter {

	/**
	 * 将参数转换为日期类型（java.util.Date）
	 */
	@Override
	public void convert(Converter converter, Map<String, Object> params) {
		List<ToDate> toDates = converter.getToDates();
		if (CollectionUtils.isEmpty(toDates)) {
			return;
		}
		for (Iterator<ToDate> it = toDates.iterator(); it.hasNext();) {
			ToDate todate = it.next();
			String paramsConfig = todate.getParams();
			String formatter = todate.getFormatter();
			if (StringUtils.isNotBlank(paramsConfig)) {
				String paramNames[] = paramsConfig.split(",");
				for (int i = 0; i < paramNames.length; i++) {
					String paramName = paramNames[i].trim();
					if ("*".equals(paramName)) {
						Set<String> set = params.keySet();
						for (Iterator<String> nit = set.iterator(); nit.hasNext();) {
							paramName = nit.next();
							Object paramValue = params.get(paramName);
							Date date = null;
							if (paramValue != null) {
								try {
									date = DateUtils.parse(paramValue, formatter);
								} catch (ParseException e) {
									e.printStackTrace();
									final String msg = String.format("将参数%s：%s，按模板：%s，转换为日期对象失败", paramName,
											paramValue.toString(), formatter);
									throw new ConvertException(msg, e);
								}
							}
							params.put(paramName, date);
						}
						break;
					} else if (params.containsKey(paramName)) {
						Object paramValue = params.get(paramName);
						Date date = null;
						if (paramValue != null) {
							try {
								date = DateUtils.parse(paramValue, formatter);
							} catch (ParseException e) {
								e.printStackTrace();
								String msg = String.format("将参数%s：%s，按模板：%s，转换为日期对象失败", paramName,
										paramValue.toString(), formatter);
								throw new ConvertException(msg, e);
							}
						}
						params.put(paramName, date);
					}
				}
			}
		}
	}

}
