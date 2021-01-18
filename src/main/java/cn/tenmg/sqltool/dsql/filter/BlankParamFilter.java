package cn.tenmg.sqltool.dsql.filter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.tenmg.sqltool.config.model.Filter;
import cn.tenmg.sqltool.config.model.filter.Blank;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 空白字符串参数过滤器
 * 
 * @author 赵伟均
 *
 */
public class BlankParamFilter implements ParamFilter {

	/**
	 * 将空白字符串参数过滤掉
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(Filter filter, Map<String, ?> params) {
		List<Blank> blanks = filter.getBlanks();
		if (CollectionUtils.isEmpty(blanks)) {
			return;
		}
		for (Iterator<Blank> it = blanks.iterator(); it.hasNext();) {
			Blank blank = it.next();
			String paramsConfig = blank.getParams();
			if (StringUtils.isNotBlank(paramsConfig)) {
				String paramNames[] = paramsConfig.split(",");
				for (int i = 0; i < paramNames.length; i++) {
					String paramName = paramNames[i].trim();
					if ("*".equals(paramName)) {
						Iterator<?> eit = params.entrySet().iterator();
						while (eit.hasNext()) {
							Entry<String, ?> e = (Entry<String, ?>) eit.next();
							Object paramValue = e.getValue();
							if (paramValue == null || StringUtils.isBlank(paramValue.toString())) {
								eit.remove();
							}
						}
						break;
					} else if (params.containsKey(paramName)) {
						Object paramValue = params.get(paramName);
						if (paramValue == null || StringUtils.isBlank(paramValue.toString())) {
							params.remove(paramName);
						}
					}
				}
			}
		}
	}

}
