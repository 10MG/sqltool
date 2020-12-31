package cn.tenmg.sqltool.factory;

import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.SqltoolFactory;
import cn.tenmg.sqltool.config.model.Dsql;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.dsql.utils.DSQLUtils;

/**
 * 抽象Sqltool工厂。封装了Sqltool工厂的基本功能
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractSqltoolFactory implements SqltoolFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8235596978687106626L;

	protected final Map<String, Dsql> dsqls = new HashMap<String, Dsql>();

	@Override
	public String getScript(String id) {
		Dsql sql = dsqls.get(id);
		if (sql == null) {
			return null;
		}
		return sql.getScript();
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
		Dsql obj = dsqls.get(dsql);
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
		return DSQLUtils.parse(dsql.getScript(), params);
	}

}
