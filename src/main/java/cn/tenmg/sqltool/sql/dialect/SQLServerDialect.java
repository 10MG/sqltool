package cn.tenmg.sqltool.sql.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.utils.JdbcUtils;

public class SQLServerDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = -253323537155093627L;

	private static final String UPDATE_SET_TEMPLATE = "${columnName} = ?",
			UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = ISNULL(?, ${columnName})";

	private static final String INSERT_IF_NOT_EXISTS = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String SAVE = "MERGE INTO ${tableName} X USING (SELECT ${fields} FROM DUAL) Y ON (${condition}) WHEN MATCHED THEN UPDATE SET ${sets} WHEN NOT MATCHED THEN INSERT (${columns}) VALUES(${values})";

	private static final String FIELDS = "fields", CONDITION = "condition", SPACE = " ";

	private static final List<String> EXT_SQL_TEMPLATE_PARAM_NAMES = Arrays.asList(FIELDS, CONDITION),
			NEEDS_COMMA_PARAM_NAMES = Arrays.asList(FIELDS, COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "X.${columnName} = Y.${columnName}",
			SET_IF_NOT_NULL_TEMPLATE = "X.${columnName} = ISNULL(Y.${columnName}, X.${columnName})";

	private static final String SQLTOOL_COLUMN = " 1 SQLTOOL_COLUMN, ",
			SUBQUERY_START = "SELECT" + SQLTOOL_COLUMN + "SQLTOOL.* FROM (\n", SUBQUERY_END = ") SQLTOOL",
			ORDER_BY = "\nORDER BY SQLTOOL_COLUMN", PAGE_WRAP_END = " OFFSET %d ROW FETCH NEXT %d ROW ONLY";

	private static final SQLServerDialect INSTANCE = new SQLServerDialect();

	public static final SQLServerDialect getInstance() {
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
		int selectIndex = sqlMetaData.getSelectIndex();
		if (selectIndex < 0) {// 正常情况下selectIndex不可能<0，但如果用户的确写错了，这里直接返回错误的SQL
			return sql;
		} else {
			int offsetIndex = sqlMetaData.getOffsetIndex(), length = sqlMetaData.getLength(),
					embedEndIndex = sqlMetaData.getEmbedEndIndex();
			if (offsetIndex > 0) {// 有OFFSET子句，直接包装子查询并追加行数限制条件
				int embedStartIndex = sqlMetaData.getEmbedStartIndex();
				if (embedStartIndex > 0) {
					if (embedEndIndex < length) {
						return sql.substring(0, embedStartIndex).concat(SUBQUERY_START)
								.concat(sql.substring(embedStartIndex, embedEndIndex)).concat(SUBQUERY_END)
								.concat(ORDER_BY).concat(pageEnd(pageSize, currentPage))
								.concat(sql.substring(embedEndIndex));
					} else {
						return sql.substring(0, embedStartIndex).concat(SUBQUERY_START)
								.concat(sql.substring(embedStartIndex)).concat(SUBQUERY_END).concat(ORDER_BY)
								.concat(pageEnd(pageSize, currentPage));
					}
				} else {
					if (embedEndIndex < length) {
						return SUBQUERY_START.concat(sql.substring(0, embedEndIndex)).concat(SUBQUERY_END)
								.concat(ORDER_BY).concat(pageEnd(pageSize, currentPage))
								.concat(sql.substring(embedEndIndex));
					} else {
						return SUBQUERY_START.concat(sql).concat(SUBQUERY_END).concat(ORDER_BY)
								.concat(pageEnd(pageSize, currentPage));
					}
				}
			} else {
				int orderByIndex = sqlMetaData.getOrderByIndex();
				if (orderByIndex > 0) {// 没有OFFSET子句但有ORDER BY子句，直接末尾追加行数限制条件
					if (embedEndIndex > 0 && embedEndIndex < length) {
						return sql.substring(0, embedEndIndex).concat(pageEnd(pageSize, currentPage))
								.concat(sql.substring(embedEndIndex));
					} else {
						return sql.concat(pageEnd(pageSize, currentPage));
					}
				} else {// 没有OFFSET子句也没有ORDER BY子句，增加一常量列并按此列排序，再追加行数限制条件
					int selectEndIndex = selectIndex + embedEndIndex;
					if (embedEndIndex > 0 && embedEndIndex < length) {
						return sql.substring(0, selectEndIndex).concat(SQLTOOL_COLUMN)
								.concat(sql.substring(selectEndIndex, embedEndIndex)).concat(ORDER_BY)
								.concat(pageEnd(pageSize, currentPage)).concat(sql.substring(embedEndIndex));
					} else {
						return sql.substring(0, selectEndIndex).concat(SQLTOOL_COLUMN)
								.concat(sql.substring(selectEndIndex)).concat(ORDER_BY)
								.concat(pageEnd(pageSize, currentPage));
					}
				}
			}
		}
	}

	private static String pageEnd(int pageSize, long currentPage) {
		return String.format(PAGE_WRAP_END, (currentPage - 1) * pageSize, pageSize);
	}

}
