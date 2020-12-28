package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.utils.JDBCUtils;

/**
 * PostgreSQL方言
 * 
 * @author 赵伟均
 *
 */
public class PostgreSQLDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6822267270540270971L;

	private static final String INSERT_IF_NOT_EXISTS = "INSERT INTO ${tableName} (${columns}) VALUES (${values}) ON CONFLICT(${ids}) DO NOTHING";

	private static final String SAVE = "INSERT INTO ${tableName} AS X(${columns}) VALUES (${values}) ON CONFLICT(${ids}) DO UPDATE SET ${sets}";

	private static final String IDS = "ids";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(IDS),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "${columnName}=EXCLUDED.${columnName}",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName}=COALESCE(EXCLUDED.${columnName}, X.${columnName})";

	private static class InstanceHolder {
		private static final PostgreSQLDialect INSTANCE = new PostgreSQLDialect();
	}

	public static final PostgreSQLDialect getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private PostgreSQLDialect() {
		super();
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
	void handleColumn(String columnName, Map<String, StringBuilder> templateParams) {
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append(JDBCUtils.PARAM_MARK);
	}

	@Override
	void handleIdColumn(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {
		StringBuilder ids = templateParams.get(IDS);
		if (notFirst) {
			ids.append(JDBCUtils.COMMA_SPACE);
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
