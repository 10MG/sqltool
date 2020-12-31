package cn.tenmg.sqltool.sql.parser;

import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.utils.EntityUtils;

/**
 * 抽象数据操纵语言解析器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractDMLParser implements DMLParser {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1468279410790030572L;

	private static final class DMLCacheHolder {
		private static volatile Map<String, DML> CACHE = new HashMap<String, DML>();
	}

	protected static DML getCachedDML(String key) {
		return DMLCacheHolder.CACHE.get(key);
	}

	protected static synchronized void cacheDML(String key, DML dml) {
		DMLCacheHolder.CACHE.put(key, dml);
	}

	protected abstract <T> void parseDML(DML dml, Class<T> type, String tableName);

	@Override
	public <T> DML parse(Class<T> type) {
		String className = this.getClass().getSimpleName();
		String key = type.getName().concat(className.substring(0, className.length() - 6));
		DML dml = DMLCacheHolder.CACHE.get(key);
		if (dml == null) {
			dml = new DML();
			parseDML(dml, type, EntityUtils.getTableName(type));
			cacheDML(key, dml);
		}
		return dml;
	}

}
