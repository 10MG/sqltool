package cn.tenmg.sqltool.utils;

import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.config.annotion.Table;
import cn.tenmg.sqltool.sql.meta.EntityMeta;

/**
 * 实体工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class EntityUtils {

	private static final class EntityMetaCacheHolder {
		private static volatile Map<Class<?>, EntityMeta> CACHE = new HashMap<Class<?>, EntityMeta>();
	}

	public static EntityMeta getCachedEntityMeta(Class<?> type) {
		return EntityMetaCacheHolder.CACHE.get(type);
	}

	public static synchronized void cacheEntityMeta(Class<?> type, EntityMeta entityMeta) {
		EntityMetaCacheHolder.CACHE.put(type, entityMeta);
	}

	public static final String getTableName(Class<?> type) {
		Table table = type.getAnnotation(Table.class);
		String tableName;
		if (table != null) {
			tableName = table.name();
		} else {
			tableName = StringUtils.camelToUnderline(type.getSimpleName(), true);
		}
		return tableName;
	}
}
