package cn.tenmg.sqltool.factory;

import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.SqltoolFactory;
import cn.tenmg.sqltool.config.model.Dsql;
import cn.tenmg.sqltool.dsql.Sql;
import cn.tenmg.sqltool.dsql.utils.DsqlUtils;

/**
 * 虚Sqltool工厂。封装了Sqltool工厂的基本功能
 * 
 * @author 赵伟均
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
	public Sql parse(String dsql, Object... params) {
		HashMap<String, Object> paramsMap = new HashMap<String, Object>();
		if (params != null) {
			for (int i = 0; i < params.length - 1; i++) {
				paramsMap.put(params[i].toString(), params[++i]);
			}
		}
		return parse(dsql, paramsMap);
	}

	@Override
	public Sql parse(String dsql, Map<String, ?> params) {
		Sql sql = null;
		Dsql obj = dsqls.get(dsql);
		if (obj == null) {
			sql = DsqlUtils.parse(dsql, params);
		} else {
			sql = parse(obj, params);
		}
		return sql;
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
	protected Sql parse(Dsql dsql, Map<String, ?> params) {
		return DsqlUtils.parse(dsql.getScript(), params);
	}

}
