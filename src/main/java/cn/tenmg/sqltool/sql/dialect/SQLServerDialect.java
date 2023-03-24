package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sql.paging.dialect.SQLServerPagingDialect;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * SQLServer方言
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.2.4
 */
public class SQLServerDialect extends AbstractSQLDialect {

	private static final String UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = ISNULL(?, ${columnName})",
			INSERT_IF_NOT_EXISTS_TEMPLATE = "MERGE INTO ${tableName} X USING (SELECT ${fields}) Y ON (${condition}) WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values});",
			SAVE_TEMPLATE = "MERGE INTO ${tableName} X USING (SELECT ${fields}) Y ON (${condition}) WHEN MATCHED THEN UPDATE SET ${sets} WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values});",
			SET_TEMPLATE = "X.${columnName} = Y.${columnName}", FIELDS = "fields", CONDITION = "condition", SPACE = " ",
			SET_IF_NOT_NULL_TEMPLATE = "X.${columnName} = ISNULL(Y.${columnName}, X.${columnName})";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(FIELDS, CONDITION),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(FIELDS, COLUMNS, VALUES);

	private static final SQLServerDialect INSTANCE = new SQLServerDialect();

	private SQLServerDialect() {
		super();
	}

	public static final SQLServerDialect getInstance() {
		return INSTANCE;
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
		templateParams.get(FIELDS).append(SQLUtils.PARAM_MARK).append(SPACE).append(columnName);
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append("Y.").append(columnName);
	}

	@Override
	void handleIdColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst) {
		StringBuilder condition = templateParams.get(CONDITION);
		if (notFirst) {
			condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
		}
		condition.append("X.").append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE).append("Y.")
				.append(columnName);
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
		return SQLServerPagingDialect.getInstance();
	}

}
