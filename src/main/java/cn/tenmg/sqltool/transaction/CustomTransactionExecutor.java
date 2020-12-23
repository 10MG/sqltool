package cn.tenmg.sqltool.transaction;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.SqltoolFactory;
import cn.tenmg.sqltool.dsql.Sql;
import cn.tenmg.sqltool.dsql.utils.DsqlUtils;
import cn.tenmg.sqltool.exception.IllegalCallException;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.JdbcSql;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SqlExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSqlExecuter;
import cn.tenmg.sqltool.sql.executer.GetSqlExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSqlExecuter;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JSONUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

/**
 * 自定义事务执行器
 * 
 * @author 赵伟均
 *
 */
public class CustomTransactionExecutor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1728127905781636407L;

	private final static Logger log = Logger.getLogger(CustomTransactionExecutor.class);

	private static ThreadLocal<Connection> currentConnection = new ThreadLocal<Connection>();

	private static ThreadLocal<SQLDialect> currentSQLDialect = new ThreadLocal<SQLDialect>();

	private SqltoolFactory sqltoolFactory;

	private boolean showSql = true;

	private int defaultBatchSize = 500;

	public SqltoolFactory getSqltoolFactory() {
		return sqltoolFactory;
	}

	public void setSqltoolFactory(SqltoolFactory sqltoolFactory) {
		this.sqltoolFactory = sqltoolFactory;
	}

	public boolean isShowSql() {
		return showSql;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	public int getDefaultBatchSize() {
		return defaultBatchSize;
	}

	public void setDefaultBatchSize(int defaultBatchSize) {
		this.defaultBatchSize = defaultBatchSize;
	}

	/**
	 * 开始事务
	 * 
	 * @param options
	 *            数据库配置
	 */
	public void beginTransaction(Map<String, String> options) {
		currentSQLDialect.set(SQLDialectUtils.getSQLDialect(options));
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			currentConnection.set(con);
		} catch (SQLException e) {
			JdbcUtils.close(con);
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		}
	}

	/**
	 * 插入操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int insert(T obj) throws SQLException {
		DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return JdbcUtils.execute(currentConnection.get(), dml.getSql(), params, ExecuteUpdateSqlExecuter.getInstance(),
				showSql);
	}

	/**
	 * 插入操作（实体对象集为空则直接返回null）。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int insert(List<T> rows) throws SQLException {
		return JdbcUtils.insert(currentConnection.get(), showSql, rows);
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int save(T obj) throws SQLException {
		JdbcSql jdbcSql = currentSQLDialect.get().save(obj);
		return JdbcUtils.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
				ExecuteUpdateSqlExecuter.getInstance(), showSql);
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int save(T obj, String... hardFields) throws SQLException {
		JdbcSql jdbcSql = currentSQLDialect.get().save(obj, hardFields);
		return JdbcUtils.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
				ExecuteUpdateSqlExecuter.getInstance(), showSql);
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int save(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.save(currentConnection.get(), showSql, rows,
				currentSQLDialect.get().save(rows.get(0).getClass()));
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int save(List<T> rows, String... hardFields) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.save(currentConnection.get(), showSql, rows,
				currentSQLDialect.get().save(rows.get(0).getClass(), hardFields));
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardSave(T obj) throws SQLException {
		JdbcSql jdbcSql = currentSQLDialect.get().hardSave(obj);
		return JdbcUtils.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
				ExecuteUpdateSqlExecuter.getInstance(), showSql);
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardSave(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.hardSave(currentConnection.get(), currentSQLDialect.get(), showSql, rows);
	}

	/**
	 * 从数据库查询并组装实体对象。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象
	 * @throws SQLException
	 *             SQL异常
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(T obj) throws SQLException {
		Class<T> type = (Class<T>) obj.getClass();
		DML dml = GetDMLParser.getInstance().parse(type);
		return JdbcUtils.execute(currentConnection.get(), dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()),
				new GetSqlExecuter<T>(type), showSql);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这事将返回结果集中的第1行第1列的值。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> T get(Class<T> type, String dsql, Object... params) throws SQLException {
		return get(currentConnection.get(), sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行第1列的值。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> T get(Class<T> type, String dsql, Map<String, ?> params) throws SQLException {
		return get(currentConnection.get(), sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 从数据库查询并组装实体对象列表。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象列表
	 * @throws SQLException
	 *             SQL异常
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> List<T> select(T obj) throws SQLException {
		Class<T> type = (Class<T>) obj.getClass();
		DML dml = GetDMLParser.getInstance().parse(type);
		return JdbcUtils.execute(currentConnection.get(), dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()),
				new SelectSqlExecuter<T>(type), showSql);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params) throws SQLException {
		return select(currentConnection.get(), sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> List<T> select(Class<T> type, String dsql, Map<String, ?> params)
			throws SQLException {
		return select(currentConnection.get(), sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	public boolean execute(String dsql, Object... params) {
		return this.execute(sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	public boolean execute(String dsql, Map<String, ?> params) {
		return this.execute(sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	public int executeUpdate(String dsql, Object... params) {
		return executeUpdate(sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。该方法不自动提交事务，且调用前需要先调用beginTransaction方法开启事务，之后在合适的时机还需要调用commit方法提交事务。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	public int executeUpdate(String dsql, Map<String, ?> params) {
		return executeUpdate(sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 事务回滚。在业务方法发生异常时调用。
	 */
	public void rollback() {
		Connection con = getCurrentConnection();
		try {
			con.rollback();
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
			currentConnection.remove();
		}
	}

	/**
	 * 提交事务
	 */
	public void commit() {
		Connection con = getCurrentConnection();
		try {
			con.commit();
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
			currentConnection.remove();
		}
	}

	private <T extends Serializable> T get(Connection con, Sql sql, Class<T> type) throws SQLException {
		return this.execute(con, sql, new GetSqlExecuter<T>(type));
	}

	private <T extends Serializable> List<T> select(Connection con, Sql sql, Class<T> type) throws SQLException {
		return this.execute(con, sql, new SelectSqlExecuter<T>(type));
	}

	private boolean execute(Sql sql) {
		Connection con = getCurrentConnection();
		JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
		PreparedStatement ps = null;
		boolean rs = false;
		try {
			String script = jdbcSql.getScript();
			List<Object> params = jdbcSql.getParams();
			ps = con.prepareStatement(script);
			JdbcUtils.setParams(ps, params);
			if (showSql) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(script).append(JdbcUtils.LINE_SEPARATOR).append("Params: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			rs = ps.execute();
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(ps);
		}
		return rs;
	}

	private int executeUpdate(Sql sql) {
		Connection con = getCurrentConnection();
		JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
		PreparedStatement ps = null;
		int count = 0;
		try {
			String script = jdbcSql.getScript();
			List<Object> params = jdbcSql.getParams();
			ps = con.prepareStatement(script);
			JdbcUtils.setParams(ps, params);
			if (showSql) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(script).append(JdbcUtils.LINE_SEPARATOR).append("Params: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			count = ps.executeUpdate();
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(ps);
		}
		return count;
	}

	private <T> T execute(Connection con, Sql sql, SqlExecuter<T> sqlExecuter) throws SQLException {
		JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
		return JdbcUtils.execute(con, jdbcSql.getScript(), jdbcSql.getParams(), sqlExecuter, showSql);
	}

	private static Connection getCurrentConnection() {
		Connection con = currentConnection.get();
		if (con == null) {
			throw new IllegalCallException("You must call beginTransaction first before you call this method");
		}
		return con;
	}
}
