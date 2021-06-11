package cn.tenmg.sqltool.transaction;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.tenmg.sqltool.DSQLFactory;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSQLExecuter;
import cn.tenmg.sqltool.sql.executer.GetSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.parser.DeleteDMLParser;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.sql.utils.SQLUtils;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JSONUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;

public class TransactionExecutor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4415310449248911047L;

	private static final Logger log = LogManager.getLogger(TransactionExecutor.class);

	private boolean showSql = true;

	private DSQLFactory DSQLFactory;

	private SQLDialect dialect;

	public TransactionExecutor(boolean showSql, DSQLFactory DSQLFactory, SQLDialect dialect) {
		super();
		this.showSql = showSql;
		this.DSQLFactory = DSQLFactory;
		this.dialect = dialect;
	}

	/**
	 * 插入操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public int insert(Object obj) throws SQLException {
		DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
		String sql = dml.getSql();
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		if (showSql && log.isInfoEnabled()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Execute SQL: ").append(sql).append(JdbcUtils.COMMA_SPACE).append("parameters: ")
					.append(JSONUtils.toJSONString(params));
			log.info(sb.toString());
		}
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql, params,
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 插入操作（实体对象集为空则直接返回null）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int insert(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			String sql = dml.getSql();
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			return JdbcUtils.executeBatch(CurrentConnectionHolder.get(), showSql, rows, InsertDMLParser.getInstance());
		}
	}

	/**
	 * 软更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int update(T obj) throws SQLException {
		SQL sql = dialect.update(obj);
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql.getScript(), sql.getParams(),
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 部分硬更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int update(T obj, String... hardFields) throws SQLException {
		SQL sql = dialect.update(obj, hardFields);
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql.getScript(), sql.getParams(),
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 软更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int update(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.update(CurrentConnectionHolder.get(), showSql, rows, dialect.update(rows.get(0).getClass()));
	}

	/**
	 * 部分硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int update(List<T> rows, String... hardFields) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.update(CurrentConnectionHolder.get(), showSql, rows,
				dialect.update(rows.get(0).getClass(), hardFields));
	}

	/**
	 * 硬更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardUpdate(T obj) throws SQLException {
		DML dml = UpdateDMLParser.getInstance().parse(obj.getClass());
		PreparedStatement ps = null;
		try {
			String sql = dml.getSql();
			ps = CurrentConnectionHolder.get().prepareStatement(sql);
			List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
			JdbcUtils.setParams(ps, params);
			if (showSql && log.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(sql).append(JdbcUtils.COMMA_SPACE).append("parameters: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			return ps.executeUpdate();
		} catch (SQLException e) {
			throw e;
		} finally {
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			JdbcUtils.close(ps);
		}
	}

	/**
	 * 硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardUpdate(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.hardUpdate(CurrentConnectionHolder.get(), showSql, rows);
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int save(T obj) throws SQLException {
		SQL sql = dialect.save(obj);
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql.getScript(), sql.getParams(),
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
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
		SQL sql = dialect.save(obj, hardFields);
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql.getScript(), sql.getParams(),
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
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
		return JdbcUtils.save(CurrentConnectionHolder.get(), showSql, rows, dialect.save(rows.get(0).getClass()));
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
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
		return JdbcUtils.save(CurrentConnectionHolder.get(), showSql, rows,
				dialect.save(rows.get(0).getClass(), hardFields));
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardSave(T obj) throws SQLException {
		SQL sql = dialect.hardSave(obj);
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, sql.getScript(), sql.getParams(),
				ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int hardSave(List<T> rows) throws SQLException {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return JdbcUtils.hardSave(CurrentConnectionHolder.get(), dialect, showSql, rows);
	}

	/**
	 * 删除操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int delete(T obj) throws SQLException {
		DML dml = DeleteDMLParser.getInstance().parse(obj.getClass());
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, dml.getSql(),
				JdbcUtils.getParams(obj, dml.getFields()), ExecuteUpdateSQLExecuter.getInstance(), showSql);
	}

	/**
	 * 删除操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public <T extends Serializable> int delete(List<T> rows) throws SQLException {
		return JdbcUtils.executeBatch(CurrentConnectionHolder.get(), showSql, rows, DeleteDMLParser.getInstance());
	}

	/**
	 * 从数据库查询并组装实体对象
	 * 
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
		return JdbcUtils.execute(CurrentConnectionHolder.get(), null, dml.getSql(),
				JdbcUtils.getParams(obj, dml.getFields()), new GetSQLExecuter<T>(type), showSql);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这事将返回结果集中的第1行第1列的值
	 * 
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
		return get(DSQLFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行第1列的值
	 * 
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
		return get(DSQLFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
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
	public <T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params) throws SQLException {
		return select(DSQLFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
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
		return select(DSQLFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 * @throws SQLException
	 *             SQL异常
	 */
	public boolean execute(String dsql, Object... params) throws SQLException {
		return this.execute(DSQLFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 * @throws SQLException
	 *             SQL异常
	 */
	public boolean execute(String dsql, Map<String, ?> params) throws SQLException {
		return this.execute(DSQLFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public int executeUpdate(String dsql, Object... params) throws SQLException {
		return executeUpdate(DSQLFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public int executeUpdate(String dsql, Map<String, ?> params) throws SQLException {
		return executeUpdate(DSQLFactory.parse(dsql, params));
	}

	private boolean execute(NamedSQL namedSQL) throws SQLException {
		SQL sql = SQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		PreparedStatement ps = null;
		boolean rs = false;
		Connection con = CurrentConnectionHolder.get();
		try {
			String script = sql.getScript();
			List<Object> params = sql.getParams();
			ps = con.prepareStatement(script);
			JdbcUtils.setParams(ps, sql.getParams());
			if (showSql && log.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(script).append(JdbcUtils.COMMA_SPACE).append("parameters: ")
						.append(JSONUtils.toJSONString(params));
				String id = namedSQL.getId();
				if (id != null) {
					sb.append(JdbcUtils.COMMA_SPACE).append("id: ").append(id);
				}
				log.info(sb.toString());
			}
			rs = ps.execute();
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw e;
		} finally {
			JdbcUtils.close(ps);
		}
		return rs;
	}

	private int executeUpdate(NamedSQL namedSQL) throws SQLException {
		SQL sql = SQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		PreparedStatement ps = null;
		int count = 0;
		Connection con = CurrentConnectionHolder.get();
		try {
			String script = sql.getScript();
			List<Object> params = sql.getParams();
			ps = con.prepareStatement(script);
			JdbcUtils.setParams(ps, params);
			if (showSql && log.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(script).append(JdbcUtils.COMMA_SPACE).append("parameters: ")
						.append(JSONUtils.toJSONString(params));
				String id = namedSQL.getId();
				if (id != null) {
					sb.append(JdbcUtils.COMMA_SPACE).append("id: ").append(id);
				}
				log.info(sb.toString());
			}
			count = ps.executeUpdate();
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw e;
		} finally {
			JdbcUtils.close(ps);
		}
		return count;
	}

	private <T extends Serializable> T get(NamedSQL namedSQL, Class<T> type) throws SQLException {
		SQL sql = SQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		return JdbcUtils.execute(CurrentConnectionHolder.get(), namedSQL.getId(), sql.getScript(), sql.getParams(),
				new GetSQLExecuter<T>(type), showSql);
	}

	private <T extends Serializable> List<T> select(NamedSQL namedSQL, Class<T> type) throws SQLException {
		SQL sql = SQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		return JdbcUtils.execute(CurrentConnectionHolder.get(), namedSQL.getId(), sql.getScript(), sql.getParams(),
				new SelectSQLExecuter<T>(type), showSql);
	}
}
