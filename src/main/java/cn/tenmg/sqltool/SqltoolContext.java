package cn.tenmg.sqltool;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.dsql.JdbcSql;
import cn.tenmg.sqltool.dsql.Sql;
import cn.tenmg.sqltool.dsql.utils.DsqlUtils;
import cn.tenmg.sqltool.exception.IllegalCallException;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.NosuitableSQLDialectExeption;
import cn.tenmg.sqltool.exception.TransactionException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SqlExecuter;
import cn.tenmg.sqltool.sql.dialect.MySQLDialect;
import cn.tenmg.sqltool.sql.executer.ExecuteSqlExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSqlExecuter;
import cn.tenmg.sqltool.sql.executer.GetSqlExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSqlExecuter;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JSONUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;

/**
 * sqltool上下文
 * 
 * @author 赵伟均
 *
 */
public class SqltoolContext implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -840162594918947327L;

	private final static Logger log = Logger.getLogger(SqltoolContext.class);

	private static String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

	private static final int DEFAULT_BATCH_SIZE = 500;

	private SqltoolFactory sqltoolFactory;

	private boolean showSql = true;

	private int defaultBatchSize = DEFAULT_BATCH_SIZE;

	private static ThreadLocal<Connection> currentConnection = new ThreadLocal<Connection>();

	public int getDefaultBatchSize() {
		return defaultBatchSize;
	}

	public void setDefaultBatchSize(int defaultBatchSize) {
		this.defaultBatchSize = defaultBatchSize;
	}

	public SqltoolContext() {
	}

	public SqltoolContext(SqltoolFactory sqltoolFactory) {
		super();
		this.sqltoolFactory = sqltoolFactory;
	}

	public SqltoolContext(SqltoolFactory sqltoolFactory, boolean showSql) {
		super();
		this.sqltoolFactory = sqltoolFactory;
		this.showSql = showSql;
	}

	public SqltoolContext(SqltoolFactory sqltoolFactory, boolean showSql, int defaultBatchSize) {
		super();
		this.sqltoolFactory = sqltoolFactory;
		this.showSql = showSql;
		this.defaultBatchSize = defaultBatchSize;
	}

	/**
	 * 插入操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	public int insert(Map<String, String> options, Object obj) {
		DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return execute(options, dml.getSql(), params, ExecuteUpdateSqlExecuter.getInstance());
	}

	/**
	 * 插入操作（实体对象集为空则直接返回null）
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回每批的受影响行数
	 */
	public int[] insert(Map<String, String> options, List<Object> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return null;
		} else {
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			return executeBatch(options, dml.getSql(), rows, dml.getFields());
		}
	}

	/**
	 * 使用默认批容量执行批量插入操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public void insertBatch(Map<String, String> options, List<Object> rows) {
		insertBatch(options, rows, DEFAULT_BATCH_SIZE);
	}

	/**
	 * 
	 * 批量插入操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	public void insertBatch(Map<String, String> options, List<Object> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			executeBatch(options, dml.getSql(), rows, dml.getFields(), batchSize);
		}
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int save(Map<String, String> options, T obj) {
		JdbcSql jdbcSql = getSQLDialect(options).save(obj);
		return execute(options, jdbcSql.getScript(), jdbcSql.getParams(), ExecuteUpdateSqlExecuter.getInstance());
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void save(Map<String, String> options, List<T> rows) {
		saveTransaction(options, rows);
	}

	/**
	 * 使用默认批容量批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows) {
		executeBatch(options, rows, defaultBatchSize);
	}

	/**
	 * 
	 * 批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows, int batchSize) {
		executeBatch(options, rows, batchSize);
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int save(Map<String, String> options, T obj, String[] hardFields) {
		JdbcSql jdbcSql = getSQLDialect(options).save(obj, hardFields);
		return execute(options, jdbcSql.getScript(), jdbcSql.getParams(), ExecuteUpdateSqlExecuter.getInstance());
	}

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 */
	public <T extends Serializable> void save(Map<String, String> options, List<T> rows, String[] hardFields) {
		saveTransaction(options, rows, hardFields);
	}

	/**
	 * 使用默认批容量批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 */
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows, String[] hardFields) {
		executeBatch(options, rows, hardFields, defaultBatchSize);
	}

	/**
	 * 
	 * 批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @param batchSize
	 *            批容量
	 */
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows, String[] hardFields,
			int batchSize) {
		executeBatch(options, rows, hardFields, batchSize);
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int hardSave(Map<String, String> options, T obj) {
		JdbcSql jdbcSql = getSQLDialect(options).hardSave(obj);
		return execute(options, jdbcSql.getScript(), jdbcSql.getParams(), ExecuteUpdateSqlExecuter.getInstance());
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void hardSave(Map<String, String> options, List<T> rows) {
		hardSaveTransaction(options, rows);
	}

	/**
	 * 使用默认批容量批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void hardSaveBatch(Map<String, String> options, List<T> rows) {
		executHardBatch(options, rows, defaultBatchSize);
	}

	/**
	 * 
	 * 批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	public <T extends Serializable> void hardSaveBatch(Map<String, String> options, List<T> rows, int batchSize) {
		executHardBatch(options, rows, batchSize);
	}

	/**
	 * 从数据库查询并组装实体对象
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(Map<String, String> options, T obj) {
		Class<T> type = (Class<T>) obj.getClass();
		DML dml = GetDMLParser.getInstance().parse(type);
		return this.execute(options, dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()),
				new GetSqlExecuter<T>(type));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这事将返回结果集中的第1行第1列的值
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
	 */
	public <T extends Serializable> T get(Map<String, String> options, Class<T> type, String dsql, Object... params) {
		return get(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行第1列的值
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
	 */
	public <T extends Serializable> T get(Map<String, String> options, Class<T> type, String dsql,
			Map<String, Object> params) {
		return get(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行的值
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
	 */
	public <T extends Serializable> List<T> select(Map<String, String> options, Class<T> type, String dsql,
			Object... params) {
		return select(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行的值
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
	 */
	public <T extends Serializable> List<T> select(Map<String, String> options, Class<T> type, String dsql,
			Map<String, Object> params) {
		return select(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	public boolean execute(Map<String, String> options, String dsql, Object... params) {
		return this.execute(options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	public boolean execute(Map<String, String> options, String dsql, Map<String, Object> params) {
		return this.execute(options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	public int executeUpdate(Map<String, String> options, String dsql, Object... params) {
		return this.executeUpdate(options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	public int executeUpdate(Map<String, String> options, String dsql, Map<String, Object> params) {
		return this.executeUpdate(options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 开始事务
	 * 
	 * @param options
	 *            数据库配置
	 */
	public void beginTransaction(Map<String, String> options) {
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
	public boolean execute(String dsql, Map<String, Object> params) {
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
	public int executeUpdate(String dsql, Map<String, Object> params) {
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

	/**
	 * 执行一个事务操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param transaction
	 *            事务对象
	 */
	public void execute(Map<String, String> options, Transaction transaction) {
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			currentConnection.set(con);
			transaction.execute(new SqltoolExecutor(showSql, sqltoolFactory, getSQLDialect(options)));
			con.commit();
		} catch (Exception e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			if (e instanceof ClassNotFoundException) {
				throw new IllegalConfigException(e);
			} else if (e instanceof SQLException) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
			throw new TransactionException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	public static class SqltoolExecutor implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -185528394193239604L;

		private boolean showSql = true;

		private SqltoolFactory sqltoolFactory;

		private SQLDialect dialect;

		public SqltoolExecutor(boolean showSql, SqltoolFactory sqltoolFactory, SQLDialect dialect) {
			super();
			this.showSql = showSql;
			this.sqltoolFactory = sqltoolFactory;
			this.dialect = dialect;
		}

		/**
		 * 插入操作
		 * 
		 * @param obj
		 *            实体对象（不能为null）
		 * @return 返回受影响行数
		 */
		public int insert(Object obj) {
			DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
			String sql = dml.getSql();
			List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
			if (showSql) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(sql).append(LINE_SEPARATOR).append("Params: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			try {
				return SqltoolContext.execute(currentConnection.get(), sql, params,
						ExecuteUpdateSqlExecuter.getInstance(), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 插入操作（实体对象集为空则直接返回null）
		 * 
		 * @param rows
		 *            实体对象集
		 * @return 返回每批的受影响行数
		 */
		public int[] insert(List<Object> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return null;
			} else {
				DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
				String sql = dml.getSql();
				if (showSql) {
					log.info("Execute SQL: ".concat(sql));
				}
				try {
					return executeBatch(showSql, currentConnection.get(), dml.getSql(), rows, dml.getFields());
				} catch (SQLException e) {
					throw new cn.tenmg.sqltool.exception.SQLException(e);
				}
			}
		}

		/**
		 * 软保存。仅对属性值不为null的字段执行插入/更新操作
		 * 
		 * @param obj
		 *            实体对象
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int save(T obj) {
			JdbcSql jdbcSql = dialect.save(obj);
			try {
				return SqltoolContext.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
						ExecuteUpdateSqlExecuter.getInstance(), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 软保存。仅对属性值不为null的字段执行插入/更新操作
		 * 
		 * @param rows
		 *            实体对象集
		 */
		public <T extends Serializable> void save(List<T> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return;
			}
			try {
				SqltoolContext.saveTransaction(showSql, dialect, currentConnection.get(), rows);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
		 * 
		 * @param obj
		 *            实体对象
		 * @param hardFields
		 *            硬保存属性
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int save(T obj, String[] hardFields) {
			JdbcSql jdbcSql = dialect.save(obj, hardFields);
			try {
				return SqltoolContext.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
						ExecuteUpdateSqlExecuter.getInstance(), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
		 * 
		 * @param rows
		 *            实体对象集
		 * @param hardFields
		 *            硬保存属性
		 */
		public <T extends Serializable> void save(List<T> rows, String[] hardFields) {
			if (CollectionUtils.isEmpty(rows)) {
				return;
			}
			try {
				SqltoolContext.saveTransaction(showSql, dialect, currentConnection.get(), rows, hardFields);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 硬保存。对所有字段执行插入/更新操作
		 * 
		 * @param obj
		 *            实体对象
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int hardSave(T obj) {
			JdbcSql jdbcSql = dialect.hardSave(obj);
			try {
				return SqltoolContext.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
						ExecuteUpdateSqlExecuter.getInstance(), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 硬保存。对所有字段执行插入/更新操作
		 * 
		 * @param rows
		 *            实体对象集
		 */
		public <T extends Serializable> void hardSave(List<T> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return;
			}
			try {
				SqltoolContext.hardSaveTransaction(showSql, dialect, currentConnection.get(), rows);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 从数据库查询并组装实体对象
		 * 
		 * @param obj
		 *            实体对象
		 * @return 返回查询到的实体对象
		 */
		@SuppressWarnings("unchecked")
		public <T extends Serializable> T get(T obj) {
			Class<T> type = (Class<T>) obj.getClass();
			DML dml = GetDMLParser.getInstance().parse(type);
			try {
				return SqltoolContext.execute(currentConnection.get(), dml.getSql(),
						JdbcUtils.getParams(obj, dml.getFields()), new GetSqlExecuter<T>(type), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
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
		 */
		public <T extends Serializable> T get(Map<String, String> options, Class<T> type, String dsql,
				Object... params) {
			return get(sqltoolFactory.parse(dsql, params), type);
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
		 */
		public <T extends Serializable> T get(Class<T> type, String dsql, Map<String, Object> params) {
			return get(sqltoolFactory.parse(dsql, params), type);
		}

		/**
		 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
		 * Date、BigDecimal类型，这时将返回结果集中的第1行的值
		 * 
		 * @param type
		 *            对象类型
		 * @param dsql
		 *            动态结构化查询语言
		 * @param params
		 *            查询参数键值集
		 * @return 返回查询到的对象列表
		 */
		public <T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params) {
			return select(sqltoolFactory.parse(dsql, params), type);
		}

		/**
		 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
		 * Date、BigDecimal类型，这时将返回结果集中的第1行的值
		 * 
		 * @param type
		 *            对象类型
		 * @param dsql
		 *            动态结构化查询语言
		 * @param params
		 *            查询参数键值集
		 * @return 返回查询到的对象列表
		 */
		public <T extends Serializable> List<T> select(Class<T> type, String dsql, Map<String, Object> params) {
			return select(sqltoolFactory.parse(dsql, params), type);
		}

		/**
		 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
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
		 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
		 * 
		 * @param dsql
		 *            动态结构化查询语言
		 * @param params
		 *            查询参数键值集
		 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
		 */
		public boolean execute(String dsql, Map<String, Object> params) {
			return this.execute(sqltoolFactory.parse(dsql, params));
		}

		/**
		 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
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
		 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作。
		 * 
		 * @param dsql
		 *            动态结构化查询语言
		 * @param params
		 *            查询参数键值集
		 * @return 返回受影响行数
		 */
		public int executeUpdate(String dsql, Map<String, Object> params) {
			return executeUpdate(sqltoolFactory.parse(dsql, params));
		}

		private boolean execute(Sql sql) {
			JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
			PreparedStatement ps = null;
			boolean rs = false;
			Connection con = currentConnection.get();
			try {
				ps = con.prepareStatement(jdbcSql.getScript());
				JdbcUtils.setParams(ps, jdbcSql.getParams());
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
			JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
			PreparedStatement ps = null;
			int count = 0;
			Connection con = currentConnection.get();
			try {
				String script = jdbcSql.getScript();
				List<Object> params = jdbcSql.getParams();
				ps = con.prepareStatement(script);
				JdbcUtils.setParams(ps, params);
				if (showSql) {
					StringBuilder sb = new StringBuilder();
					sb.append("Execute SQL: ").append(script).append(LINE_SEPARATOR).append("Params: ")
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

		private <T extends Serializable> T get(Sql sql, Class<T> type) {
			JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
			try {
				return SqltoolContext.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
						new GetSqlExecuter<T>(type), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		private <T extends Serializable> List<T> select(Sql sql, Class<T> type) {
			JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
			try {
				return SqltoolContext.execute(currentConnection.get(), jdbcSql.getScript(), jdbcSql.getParams(),
						new SelectSqlExecuter<T>(type), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}
	}

	private <T extends Serializable> T get(Map<String, String> options, Sql sql, Class<T> type) {
		return this.execute(options, sql, new GetSqlExecuter<T>(type));
	}

	private <T extends Serializable> List<T> select(Map<String, String> options, Sql sql, Class<T> type) {
		return this.execute(options, sql, new SelectSqlExecuter<T>(type));
	}

	private boolean execute(Map<String, String> options, Sql sql) {
		return this.execute(options, sql, ExecuteSqlExecuter.getInstance());
	}

	private int executeUpdate(Map<String, String> options, Sql sql) {
		return this.execute(options, sql, ExecuteUpdateSqlExecuter.getInstance());
	}

	private <T> T execute(Map<String, String> options, Sql sql, SqlExecuter<T> sqlExecuter) {
		JdbcSql jdbcSql = DsqlUtils.toJdbcSql(sql.getScript(), sql.getParams());
		return this.execute(options, jdbcSql.getScript(), jdbcSql.getParams(), sqlExecuter);
	}

	private <T> T execute(Map<String, String> options, String sql, List<Object> params, SqlExecuter<T> sqlExecuter) {
		Connection con = null;
		T result = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			result = execute(con, sql, params, sqlExecuter, showSql);
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return result;
	}

	private static <T> T execute(Connection con, String sql, List<Object> params, SqlExecuter<T> sqlExecuter,
			boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql);
			JdbcUtils.setParams(ps, params);
			if (showSql) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(sql).append(LINE_SEPARATOR).append("Params: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			rs = sqlExecuter.execute(ps);
			return sqlExecuter.execute(ps, rs);
		} catch (SQLException e) {
			throw e;
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(ps);
		}
	}

	private <T> int[] executeBatch(Map<String, String> options, String sql, List<T> rows, List<Field> fields) {
		Connection con = null;
		int[] counts = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			counts = executeBatch(showSql, con, sql, rows, fields);
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return counts;
	}

	private static <T> int[] executeBatch(boolean showSql, Connection con, String sql, List<T> rows, List<Field> fields)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sql);
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
		} catch (SQLException e) {
			ps.clearBatch();
			throw e;
		} finally {
			JdbcUtils.close(ps);
		}
		return commitBatch(con, ps);
	}

	private <T> void executeBatch(Map<String, String> options, String sql, List<T> rows, List<Field> fields,
			int batchSize) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			ps = con.prepareStatement(sql);
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				int end = (current + 1) * batchSize;
				int i = current * batchSize;
				if (end < size) {
					for (; i < end; i++) {
						addBatch(ps, rows.get(i), fields);
					}
				} else {
					for (; i < size; i++) {
						addBatch(ps, rows.get(i), fields);
					}
				}
				commitBatch(con, ps);
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T> void saveTransaction(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			saveTransaction(showSql, dialect, con, rows);
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private static <T> void saveTransaction(boolean showSql, SQLDialect dialect, Connection con, List<T> rows)
			throws SQLException {
		Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
		try {
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(dialect, con, pss, rows.get(i));
			}
		} catch (SQLException e) {
			for (Iterator<PreparedStatement> it = pss.values().iterator(); it.hasNext();) {
				PreparedStatement ps = it.next();
				ps.clearBatch();
				ps.close();
			}
			throw e;
		}
		commitBatch(con, pss, showSql);
	}

	private <T> void saveTransaction(Map<String, String> options, List<T> rows, String[] hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			saveTransaction(showSql, dialect, con, rows, hardFields);
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private static <T> void saveTransaction(boolean showSql, SQLDialect dialect, Connection con, List<T> rows,
			String[] hardFields) throws SQLException {
		Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
		try {
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(dialect, con, pss, rows.get(i), hardFields);
			}
		} catch (SQLException e) {
			for (Iterator<PreparedStatement> it = pss.values().iterator(); it.hasNext();) {
				PreparedStatement ps = it.next();
				ps.clearBatch();
				ps.close();
			}
			throw e;
		}
		commitBatch(con, pss, showSql);
	}

	private <T> void hardSaveTransaction(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			hardSaveTransaction(showSql, dialect, con, rows);
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private static <T> void hardSaveTransaction(boolean showSql, SQLDialect dialect, Connection con, List<T> rows)
			throws SQLException {
		Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
		try {
			for (int i = 0, size = rows.size(); i < size; i++) {
				addHardBatch(dialect, con, pss, rows.get(i));
			}
		} catch (SQLException e) {
			for (Iterator<PreparedStatement> it = pss.values().iterator(); it.hasNext();) {
				PreparedStatement ps = it.next();
				ps.clearBatch();
				ps.close();
			}
			throw e;
		}
		commitBatch(con, pss, showSql);
	}

	private <T> void executeBatch(Map<String, String> options, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
				int end = (current + 1) * batchSize;
				int i = current * batchSize;
				if (end < size) {
					for (; i < end; i++) {
						addBatch(dialect, con, pss, rows.get(i));
					}
				} else {
					for (; i < size; i++) {
						addBatch(dialect, con, pss, rows.get(i));
					}
				}
				commitBatch(con, pss, showSql);
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T> void executeBatch(Map<String, String> options, List<T> rows, String[] hardFields, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
				int end = (current + 1) * batchSize;
				int i = current * batchSize;
				if (end < size) {
					for (; i < end; i++) {
						addBatch(dialect, con, pss, rows.get(i), hardFields);
					}
				} else {
					for (; i < size; i++) {
						addBatch(dialect, con, pss, rows.get(i), hardFields);
					}
				}
				commitBatch(con, pss, showSql);
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T> void executHardBatch(Map<String, String> options, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				Map<String, PreparedStatement> pss = new HashMap<String, PreparedStatement>();
				int end = (current + 1) * batchSize;
				int i = current * batchSize;
				if (end < size) {
					for (; i < end; i++) {
						addHardBatch(dialect, con, pss, rows.get(i));
					}
				} else {
					for (; i < size; i++) {
						addHardBatch(dialect, con, pss, rows.get(i));
					}
				}
				commitBatch(con, pss, showSql);
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private static final <T> void addBatch(PreparedStatement ps, T row, List<Field> fields) throws SQLException {
		JdbcUtils.setParams(ps, JdbcUtils.getParams(row, fields));
		ps.addBatch();
	}

	private static final int[] commitBatch(Connection con, PreparedStatement ps) throws SQLException {
		int[] counts = null;
		try {
			counts = ps.executeBatch();
			con.commit();
			ps.clearBatch();
		} catch (SQLException e) {
			JdbcUtils.close(ps);
			throw e;
		} finally {
			JdbcUtils.close(ps);
		}
		return counts;
	}

	private static SQLDialect getSQLDialect(Map<String, String> options) {
		String url = options.get("url");
		if (url != null && url.contains("mysql")) {
			return MySQLDialect.getInstance();
		} else {
			throw new NosuitableSQLDialectExeption("There is no suitable SQL dialect provide for url: ".concat(url));
		}
	}

	private static final <T> void addBatch(SQLDialect dialect, Connection con, Map<String, PreparedStatement> pss,
			T row) throws SQLException {
		JdbcSql jdbcSql = dialect.save(row);
		String sql = jdbcSql.getScript();
		PreparedStatement ps = pss.get(sql);
		if (ps == null) {
			ps = con.prepareStatement(sql);
			pss.put(sql, ps);
		}
		JdbcUtils.setParams(ps, jdbcSql.getParams());
		ps.addBatch();
	}

	private static final <T> void addBatch(SQLDialect dialect, Connection con, Map<String, PreparedStatement> pss,
			T row, String[] hardFields) throws SQLException {
		JdbcSql jdbcSql = dialect.save(row, hardFields);
		String sql = jdbcSql.getScript();
		PreparedStatement ps = pss.get(sql);
		if (ps == null) {
			ps = con.prepareStatement(sql);
			pss.put(sql, ps);
		}
		JdbcUtils.setParams(ps, jdbcSql.getParams());
		ps.addBatch();
	}

	private static final <T> void addHardBatch(SQLDialect dialect, Connection con, Map<String, PreparedStatement> pss,
			T row) throws SQLException {
		JdbcSql jdbcSql = dialect.hardSave(row);
		String sql = jdbcSql.getScript();
		PreparedStatement ps = pss.get(sql);
		if (ps == null) {
			ps = con.prepareStatement(sql);
			pss.put(sql, ps);
		}
		JdbcUtils.setParams(ps, jdbcSql.getParams());
		ps.addBatch();
	}

	private static final void commitBatch(Connection con, Map<String, PreparedStatement> pss, boolean showSql)
			throws SQLException {
		for (Iterator<Entry<String, PreparedStatement>> it = pss.entrySet().iterator(); it.hasNext();) {
			Entry<String, PreparedStatement> entry = it.next();
			if (showSql) {
				log.info("Execute SQL: ".concat(entry.getKey()));
			}
			entry.getValue().executeBatch();
		}
		con.commit();
		for (Iterator<PreparedStatement> it = pss.values().iterator(); it.hasNext();) {
			PreparedStatement ps = it.next();
			ps.clearBatch();
			ps.close();
		}
	}

	private static Connection getCurrentConnection() {
		Connection con = currentConnection.get();
		if (con == null) {
			throw new IllegalCallException("You must call beginTransaction first before you call this method");
		}
		return con;
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
				sb.append("Execute SQL: ").append(script).append(LINE_SEPARATOR).append("Params: ")
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
				sb.append("Execute SQL: ").append(script).append(LINE_SEPARATOR).append("Params: ")
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
}
