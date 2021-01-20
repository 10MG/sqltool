package cn.tenmg.sqltool.sql.parser;

import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.sql.utils.SQLUtils;
import cn.tenmg.sqltool.utils.EntityUtils;

/**
 * 抽象数据操纵语言解析器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractDMLParser implements DMLParser {

	protected abstract <T> void parseDML(DML dml, Class<T> type, String tableName);

	@Override
	public <T> DML parse(Class<T> type) {
		String className = this.getClass().getSimpleName();
		String key = type.getName().concat(className.substring(0, className.length() - 6));
		DML dml = SQLUtils.getCachedDML(key);
		if (dml == null) {
			dml = new DML();
			parseDML(dml, type, EntityUtils.getTableName(type));
			SQLUtils.cacheDML(key, dml);
		}
		return dml;
	}

}
