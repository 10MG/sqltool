package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sql.paging.dialect.MySQLPagingDialect;
import cn.tenmg.sql.paging.utils.SQLUtils;

/**
 * MySQL 方言
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public class MySQLDialect extends AbstractSQLDialect {

	private static final String UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(?, ${columnName})",
			INSERT_IF_NOT_EXISTS_TEMPLATE = "INSERT IGNORE INTO ${tableName} (${columns}) VALUES (${values})",
			SAVE_TEMPLATE = "INSERT INTO ${tableName} (${columns}) VALUES (${values}) ON DUPLICATE KEY UPDATE ${sets}",
			SET_TEMPLATE = "${columnName} = VALUES(${columnName})",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(VALUES(${columnName}), ${columnName})";

	private static final List<String> NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final MySQLDialect INSTANCE = new MySQLDialect();

	private MySQLDialect() {
		super();
	}

	public static final MySQLDialect getInstance() {
		return INSTANCE;
	}

	@Override
	SQLPagingDialect getSQLPagingDialect() {
		return MySQLPagingDialect.getInstance();
	}

	@Override
	String getUpdateSetIfNotNullTemplate() {
		return UPDATE_SET_IF_NOT_NULL_TEMPLATE;
	}

	@Override
	List<String> getExtSQLTemplateParamNames() {
		return null;
	}

	@Override
	String getSaveSQLTemplate() {
		return SAVE_TEMPLATE;
	}

	@Override
	String getInsertIfNotExistsSQLTemplate() {
		return INSERT_IF_NOT_EXISTS_TEMPLATE;
	}

	@Override
	List<String> getNeedsCommaParamNames() {
		return NEEDS_COMMA_PARAM_NAMES;
	}

	@Override
	void handleColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams) {
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append(SQLUtils.PARAM_MARK);
	}

	@Override
	void handleIdColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {

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