package cn.tenmg.sqltool.macro;

import java.sql.Connection;
import java.util.Map;

import cn.tenmg.dsl.DSLContext;
import cn.tenmg.dsl.NamedScript;
import cn.tenmg.dsl.annotion.Macro;
import cn.tenmg.dsl.utils.DSLUtils;
import cn.tenmg.dsl.utils.StringUtils;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.data.Page;
import cn.tenmg.sqltool.sql.SQLDialect;

/**
 * 分页宏
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.5.0
 *
 */
@Macro("page")
public class Paging implements cn.tenmg.dsl.Macro {

	private static final ThreadLocal<SQLDialect> currentDialect = new ThreadLocal<SQLDialect>();

	private static final ThreadLocal<Connection> currentConnection = new ThreadLocal<Connection>();

	private static final ThreadLocal<Page<?>> currentPage = new ThreadLocal<Page<?>>();

	private static final ThreadLocal<Boolean> counted = new ThreadLocal<Boolean>(), paged = new ThreadLocal<Boolean>();

	/**
	 * 是否已生成计数查询SQL语句
	 * 
	 * @return 返回{@code true}表示已生成计数查询SQL语句，否则未生成。
	 */
	public static boolean isCounted() {
		return Boolean.TRUE.equals(counted.get());
	}

	/**
	 * 是否已生成分页查询SQL语句
	 * 
	 * @return 返回{@code true}表示已生成分页查询SQL语句，否则未生成。
	 */
	public static boolean isPaged() {
		return paged.get();
	}

	/**
	 * 初始化计数查询SQL解析环境
	 * 
	 * @param dialect
	 *            SQL方言
	 */
	public static void initCountEnv(SQLDialect dialect) {
		currentDialect.set(dialect);
		counted.set(Boolean.FALSE);
	}

	/**
	 * 清理宏执行环境
	 */
	public static void clear() {
		counted.remove();
		paged.remove();
		currentDialect.remove();
		currentConnection.remove();
		currentPage.remove();
	}

	/**
	 * 初始化分页查询SQL解析环境
	 * 
	 * @param dialect
	 *            SQL方言
	 * @param con
	 *            数据库连接
	 * @param page
	 *            分页对象
	 */
	public static void initPageEnv(SQLDialect dialect, Connection con, Page<?> page) {
		currentDialect.set(dialect);
		currentConnection.set(con);
		currentPage.set(page);
		paged.set(Boolean.FALSE);
	}

	@Override
	public boolean execute(DSLContext context, Map<String, Object> attributes, String logic, StringBuilder dslf,
			Map<String, Object> params) throws Exception {
		SQLDialect dialect = (SQLDialect) currentDialect.get();
		Page<?> page = (Page<?>) currentPage.get();
		if (page == null) {
			if (dialect == null) {// 普通查询
				dslf.insert(0, StringUtils.concat("(", logic, ")"));
			} else {// count查询
				counted.set(Boolean.TRUE);
				NamedScript namedScript = DSLUtils.parse(logic, params);
				String namedSql = namedScript.getScript();
				dslf.setLength(0);
				dslf.append(dialect.countSql(namedSql, SQLUtils.getSQLMetaData(namedSql)));
				return true;// 替换为主SQL
			}
		} else {// 分页查询
			paged.set(Boolean.TRUE);
			NamedScript namedScript = DSLUtils.parse(logic.trim(), params);
			String namedSql = namedScript.getScript();
			String pageSql = dialect.pageSql(currentConnection.get(), namedSql, namedScript.getParams(),
					SQLUtils.getSQLMetaData(namedSql), page.getPageSize(), page.getCurrentPage());
			dslf.insert(0, StringUtils.concat("(", pageSql, ")"));
		}
		return false;
	}

}
