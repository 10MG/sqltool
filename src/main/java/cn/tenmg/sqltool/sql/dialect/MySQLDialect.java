package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.utils.JdbcUtils;

/**
 * Mysql 方言
 * 
 * @author 赵伟均
 *
 */
public class MySQLDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7189284927835898553L;

	private static final String INSERT_IF_NOT_EXISTS = "INSERT IGNORE INTO ${tableName} (${columns}) VALUES (${values})";

	private static final String SAVE = "INSERT INTO ${tableName} (${columns}) VALUES (${values}) ON DUPLICATE KEY UPDATE ${sets}";

	private static final List<String> NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "${columnName}=VALUES(${columnName})",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName}=IFNULL(VALUES(${columnName}), ${columnName})";

	private static class InstanceHolder {
		private static final MySQLDialect INSTANCE = new MySQLDialect();
	}

	public static final MySQLDialect getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static final class EntityMetaCacheHolder {
		private static volatile Map<Class<?>, EntityMeta> CACHE = new HashMap<Class<?>, EntityMeta>();
	}

	protected static EntityMeta getCachedEntityMeta(Class<?> type) {
		return EntityMetaCacheHolder.CACHE.get(type);
	}

	protected static synchronized void cacheEntityMeta(Class<?> type, EntityMeta entityMeta) {
		EntityMetaCacheHolder.CACHE.put(type, entityMeta);
	}

	@Override
	List<String> getExtSQLTemplateParamNames() {
		return null;
	}

	@Override
	String getSaveSQLTemplate() {
		return SAVE;
	}

	@Override
	String getInsertIfNotExistsSQLTemplate() {
		return INSERT_IF_NOT_EXISTS;
	}

	@Override
	List<String> getNeedsCommaParamNames() {
		return NEEDS_COMMA_PARAM_NAMES;
	}

	@Override
	void handleColumn(String columnName, Map<String, StringBuilder> templateParams) {
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append(JdbcUtils.PARAM_MARK);
	}

	@Override
	void handleIdColumn(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {

	}

	@Override
	String getSetTemplate() {
		return SET_TEMPLATE;
	}

	@Override
	String getSetIfNotNullTemplate() {
		return SET_IF_NOT_NULL_TEMPLATE;
	}

}
