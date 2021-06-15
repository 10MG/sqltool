package cn.tenmg.sqltool.sql.dialect;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.utils.JdbcUtils;

/**
 * Mysql 方言
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class MySQLDialect extends AbstractSQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7189284927835898553L;

	private static final String UPDATE_SET_TEMPLATE = "${columnName} = ?",
			UPDATE_SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(?, ${columnName})";

	private static final String INSERT_IF_NOT_EXISTS = "INSERT IGNORE INTO ${tableName} (${columns}) VALUES (${values})";

	private static final String SAVE = "INSERT INTO ${tableName} (${columns}) VALUES (${values}) ON DUPLICATE KEY UPDATE ${sets}";

	private static final List<String> NEEDS_COMMA_PARAM_NAMES = Arrays.asList(COLUMNS, VALUES);

	private static final String SET_TEMPLATE = "${columnName} = VALUES(${columnName})",
			SET_IF_NOT_NULL_TEMPLATE = "${columnName} = IFNULL(VALUES(${columnName}), ${columnName})";

	private static final String PAGE_WRAP_START = "SELECT * FROM (\n", PAGE_WRAP_END = "\n) SQLTOOL",
			LIMIT = " LIMIT %d,%d";

	private static final MySQLDialect INSTANCE = new MySQLDialect();

	public static final MySQLDialect getInstance() {
		return INSTANCE;
	}

	private MySQLDialect() {
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
	void handleColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams) {
		templateParams.get(COLUMNS).append(columnName);
		templateParams.get(VALUES).append(JdbcUtils.PARAM_MARK);
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

	@Override
	public String pageSql(Connection con, String sql, Map<String, ?> params, SQLMetaData sqlMetaData, int pageSize, long currentPage) {
		int selectIndex = sqlMetaData.getSelectIndex();
		if (selectIndex < 0) {// 正常情况下selectIndex不可能<0，但如果用户的确写错了，这里直接返回错误的SQL
			return sql;
		} else {
			int length = sqlMetaData.getLength(), embedEndIndex = sqlMetaData.getEmbedEndIndex();
			if (sqlMetaData.getLimitIndex() >= 0) {
				int embedStartIndex = sqlMetaData.getEmbedStartIndex();
				if (embedStartIndex > 0) {
					if (embedEndIndex < length) {
						return sql.substring(0, embedStartIndex).concat(PAGE_WRAP_START)
								.concat(sql.substring(embedStartIndex, embedEndIndex))
								.concat(pageEnd(pageSize, currentPage)).concat(sql.substring(embedEndIndex));
					} else {
						return sql.substring(0, embedStartIndex).concat(PAGE_WRAP_START)
								.concat(sql.substring(embedStartIndex)).concat(pageEnd(pageSize, currentPage));
					}
				} else {
					if (embedEndIndex < length) {
						return PAGE_WRAP_START.concat(sql.substring(0, embedEndIndex))
								.concat(pageEnd(pageSize, currentPage)).concat(sql.substring(embedEndIndex));
					} else {
						return PAGE_WRAP_START.concat(sql).concat(pageEnd(pageSize, currentPage));
					}
				}
			} else {
				if (embedEndIndex < length) {
					return sql.substring(0, embedEndIndex).concat(generateLimit(pageSize, currentPage))
							.concat(sql.substring(embedEndIndex));
				} else {
					return sql.concat(generateLimit(pageSize, currentPage));
				}
			}
		}
	}

	private static String pageEnd(int pageSize, long currentPage) {
		return PAGE_WRAP_END.concat(generateLimit(pageSize, currentPage));
	}

	private static String generateLimit(int pageSize, long currentPage) {
		return String.format(LIMIT, (currentPage - 1) * pageSize, pageSize);
	}
}
