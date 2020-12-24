package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.utils.JdbcUtils;

/**
 * Oracle方言
 * 
 * @author 赵伟均
 *
 */
public class OracleDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6036289971714384622L;

	private static final String INSERT_IF_NOT_EXISTS = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String SAVE = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN MATCHED THEN UPDATE SET ${sets} WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String FIELDS = "fields", CONDITION = "condition", SPACE = " ";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(FIELDS, CONDITION),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(FIELDS, COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "X.${columnName}=Y.${columnName}",
			SET_IF_NOT_NULL_TEMPLATE = "X.${columnName}=NVL(Y.${columnName}, X.${columnName})";

	private static class InstanceHolder {
		private static final OracleDialect INSTANCE = new OracleDialect();
	}

	public static final OracleDialect getInstance() {
		return InstanceHolder.INSTANCE;
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
		templateParams.get(FIELDS).append(JdbcUtils.PARAM_MARK).append(SPACE).append(columnName);
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append("Y.").append(columnName);
	}

	@Override
	void handleIdColumn(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {
		StringBuilder condition = templateParams.get(CONDITION);
		if (notFirst) {
			condition.append(JdbcUtils.SPACE_AND_SPACE);
		}
		condition.append("X.").append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append("Y.").append(columnName);
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
