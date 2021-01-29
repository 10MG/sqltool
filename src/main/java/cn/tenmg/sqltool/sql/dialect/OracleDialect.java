package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.utils.JdbcUtils;

/**
 * Oracle方言
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class OracleDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6036289971714384622L;

	private static final String UPDATE_SET_TEMPLATE = "${columnName} = ?",
			UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = NVL(?, ${columnName})";

	private static final String INSERT_IF_NOT_EXISTS = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String SAVE = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN MATCHED THEN UPDATE SET ${sets} WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String FIELDS = "fields", CONDITION = "condition", SPACE = " ";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(FIELDS, CONDITION),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(FIELDS, COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "X.${columnName} = Y.${columnName}",
			SET_IF_NOT_NULL_TEMPLATE = "X.${columnName} = NVL(Y.${columnName}, X.${columnName})";

	private static final String PAGE_WRAP_START = "SELECT * FROM (SELECT SQLTOOL.*, ROWNUM SQLTOOL_RN FROM (\n",
			PAGE_WRAP_END = "\n) SQLTOOL  WHERE ROWNUM <= %d) WHERE SQLTOOL_RN > %d";

	private static class InstanceHolder {
		private static final OracleDialect INSTANCE = new OracleDialect();
	}

	public static final OracleDialect getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private OracleDialect() {
		super();
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
		templateParams.get(FIELDS).append(JdbcUtils.PARAM_MARK).append(SPACE).append(columnName);
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append("Y.").append(columnName);
	}

	@Override
	void handleIdColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {
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
	
	@Override
	String pageSql(String sql, SQLMetaData sqlMetaData, int pageSize, long currentPage) {
		return PAGE_WRAP_START.concat(sql).concat(pageWrapEnd(pageSize, currentPage));
	}

	private static String pageWrapEnd(int pageSize, long currentPage) {
		return String.format(PAGE_WRAP_END, currentPage * pageSize, (currentPage - 1) * pageSize);
	}

}
