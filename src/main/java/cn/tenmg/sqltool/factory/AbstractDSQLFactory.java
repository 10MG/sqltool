package cn.tenmg.sqltool.factory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import cn.tenmg.sqltool.DSQLFactory;
import cn.tenmg.sqltool.config.model.Converter;
import cn.tenmg.sqltool.config.model.Dsql;
import cn.tenmg.sqltool.config.model.Filter;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.dsql.converter.ParamConverter;
import cn.tenmg.sqltool.dsql.filter.ParamFilter;
import cn.tenmg.sqltool.dsql.utils.DSQLUtils;
import cn.tenmg.sqltool.utils.CollectionUtils;

/**
 * 抽象动态结构化查询语言工厂
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractDSQLFactory implements DSQLFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = -169658678380590492L;

	abstract Map<String, Dsql> getDsqls();

	@Override
	public Dsql getDsql(String id) {
		return getDsqls().get(id);
	}

	@Override
	public String getScript(String id) {
		Dsql dsql = getDsql(id);
		if (dsql == null) {
			return null;
		}
		return dsql.getScript();
	}

	@Override
	public NamedSQL parse(String dsql, Object... params) {
		HashMap<String, Object> paramsMap = new HashMap<String, Object>();
		if (params != null) {
			for (int i = 0; i < params.length - 1; i++) {
				paramsMap.put(params[i].toString(), params[++i]);
			}
		}
		return parse(dsql, paramsMap);
	}

	@Override
	public NamedSQL parse(String dsql, Map<String, ?> params) {
		NamedSQL namedSQL = null;
		Dsql obj = getDsql(dsql);
		if (obj == null) {
			namedSQL = DSQLUtils.parse(dsql, params);
		} else {
			namedSQL = parse(obj, params);
		}
		return namedSQL;
	}

	/**
	 * 根据指定的参数params分析转换动态SQL对象dsql为SQL对象
	 * 
	 * @param dsql
	 *            动态SQL配置对象
	 * @param params
	 *            参数列表
	 * @return SQL对象
	 */
	protected NamedSQL parse(Dsql dsql, Map<String, ?> params) {
		Filter filter = dsql.getFilter();
		if (filter != null) {
			ServiceLoader<ParamFilter> loader = ServiceLoader.load(ParamFilter.class);
			for (Iterator<ParamFilter> it = loader.iterator(); it.hasNext();) {
				if (CollectionUtils.isEmpty(params)) {
					break;
				}
				ParamFilter paramFilter = it.next();
				paramFilter.doFilter(filter, params);
			}
		}
		Converter converter = dsql.getConverter();
		if (converter != null) {
			Map<String, Object> paramaters = new HashMap<String, Object>();
			paramaters.putAll(params);
			ServiceLoader<ParamConverter> loader = ServiceLoader.load(ParamConverter.class);
			if (!CollectionUtils.isEmpty(params)) {
				for (Iterator<ParamConverter> it = loader.iterator(); it.hasNext();) {
					ParamConverter paramConverter = it.next();
					paramConverter.convert(converter, paramaters);
				}
			}
			params = paramaters;
		}
		return DSQLUtils.parse(dsql.getScript(), params);
	}

}
