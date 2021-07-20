package cn.tenmg.sqltool.sql.utils;

import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.sql.DML;

/**
 * 数据操作语言工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.3.0
 */
public abstract class DMLUtils {

	private static final class DMLCacheHolder {
		private static volatile Map<String, DML> CACHE = new HashMap<String, DML>();
	}

	public static DML getCachedDML(String key) {
		return DMLCacheHolder.CACHE.get(key);
	}

	public static synchronized void cacheDML(String key, DML dml) {
		DMLCacheHolder.CACHE.put(key, dml);
	}

}
