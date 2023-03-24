package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sql.paging.dialect.SQLitePagingDialect;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * SQLite方言
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.5.1
 */
public class SQLiteDialect extends AbstractSQLDialect {

	private static final String UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(?, ${columnName})",
			INSERT_IF_NOT_EXISTS_TEMPLATE = "INSERT OR IGNORE INTO ${tableName} (${columns}) VALUES (${values})",
			SAVE_TEMPLATE = "INSERT INTO ${tableName} AS X(${columns}) VALUES (${values}) ON CONFLICT(STAFF_ID) DO UPDATE SET ${sets}",
			SET_TEMPLATE = "${columnName} = EXCLUDED.${columnName}", IDS = "ids",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(EXCLUDED.${columnName}, X.${columnName})";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(IDS),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final SQLiteDialect INSTANCE = new SQLiteDialect();

	private SQLiteDialect() {
		super();
	}

	public static final SQLiteDialect getInstance() {
		return INSTANCE;
	}

	@Override
	SQLPagingDialect getSQLPagingDialect() {
		return SQLitePagingDialect.getInstance();
	}

	@Override
	String getUpdateSetIfNotNullTemplate() {
		return UPDATE_SET_IF_NOT_NULL_TEMPLATE;
	}

	@Override
	List<String> getExtSQLTemplateParamNames() {
		return EXT_SQL_TEMPLATE_PARAM_NAMES;
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
		StringBuilder ids = templateParams.get(IDS);
		if (notFirst) {
			ids.append(JDBCExecuteUtils.COMMA_SPACE);
		}
		ids.append(columnName);
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
