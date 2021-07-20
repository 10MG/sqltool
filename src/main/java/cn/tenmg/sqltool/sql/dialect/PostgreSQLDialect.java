package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sql.paging.dialect.PostgreSQLPagingDialect;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * PostgreSQL方言
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.1.1
 */
public class PostgreSQLDialect extends AbstractSQLDialect {

	private static final String UPDATE_SET_TEMPLATE = "${columnName} = ?",
			UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = COALESCE(?, ${columnName})";

	private static final String INSERT_IF_NOT_EXISTS = "INSERT INTO ${tableName} (${columns}) VALUES (${values}) ON CONFLICT(${ids}) DO NOTHING";

	private static final String SAVE = "INSERT INTO ${tableName} AS X(${columns}) VALUES (${values}) ON CONFLICT(${ids}) DO UPDATE SET ${sets}";

	private static final String IDS = "ids";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(IDS),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "${columnName} = EXCLUDED.${columnName}",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName} = COALESCE(EXCLUDED.${columnName}, X.${columnName})";

	private static final PostgreSQLDialect INSTANCE = new PostgreSQLDialect();

	private PostgreSQLDialect() {
		super();
	}

	public static final PostgreSQLDialect getInstance() {
		return INSTANCE;
	}

	@Override
	String getUpdateSetTemplate() {
		return UPDATE_SET_TEMPLATE;
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

	@Override
	SQLPagingDialect getSQLPagingDialect() {
		return PostgreSQLPagingDialect.getInstance();
	}

}
