package cn.tenmg.sqltool;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.dsql.utils.DSQLUtils;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.TransactionException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.executer.ExecuteSQLExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSQLExecuter;
import cn.tenmg.sqltool.sql.executer.GetSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JSONUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

/**
 * sqltool上下文
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class SqltoolContext implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -840162594918947327L;

	private final static Logger log = Logger.getLogger(SqltoolContext.class);

	private SqltoolFactory sqltoolFactory;

	private boolean showSql = true;

	private int defaultBatchSize = 500;

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
	public <T extends Serializable> int insert(Map<String, String> options, T obj) {
		DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return execute(options, dml.getSql(), params, ExecuteUpdateSQLExecuter.getInstance());
	}

	/**
	 * 插入操作（实体对象集为空则直接返回null）
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int insert(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			Connection con = null;
			try {
				Class.forName(options.get("driver"));
				con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
				con.setAutoCommit(false);
				return JdbcUtils.insert(con, showSql, rows);
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
	}

	/**
	 * 使用默认批容量执行批量插入操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void insertBatch(Map<String, String> options, List<T> rows) {
		insertBatch(options, rows, defaultBatchSize);
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
	public <T extends Serializable> void insertBatch(Map<String, String> options, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			executeBatch(options, dml.getSql(), rows, dml.getFields(), batchSize);
		}
	}

	/**
	 * 软更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int update(Map<String, String> options, T obj) {
		SQL sql = SQLDialectUtils.getSQLDialect(options).update(obj);
		return execute(options, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	/**
	 * 软更新操作（实体对象集为空则直接返回null）
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int update(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return update(options, showSql, rows, SQLDialectUtils.getSQLDialect(options).update(rows.get(0).getClass()));
	}

	/**
	 * 使用默认批容量执行批量软更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void updateBatch(Map<String, String> options, List<T> rows) {
		updateBatch(options, rows, defaultBatchSize);
	}

	/**
	 * 
	 * 批量软更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	public <T extends Serializable> void updateBatch(Map<String, String> options, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		updateBatch(options, rows, batchSize, SQLDialectUtils.getSQLDialect(options).update(rows.get(0).getClass()));
	}

	/**
	 * 硬更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int hardUpdate(Map<String, String> options, T obj) {
		DML dml = UpdateDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return execute(options, dml.getSql(), params, ExecuteUpdateSQLExecuter.getInstance());
	}

	/**
	 * 硬更新操作（实体对象集为空则直接返回null）
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int hardUpdate(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			Connection con = null;
			try {
				Class.forName(options.get("driver"));
				con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
				con.setAutoCommit(false);
				return JdbcUtils.hardUpdate(con, showSql, rows);
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
	}

	/**
	 * 使用默认批容量执行批量硬更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void hardUpdateBatch(Map<String, String> options, List<T> rows) {
		hardUpdateBatch(options, rows, defaultBatchSize);
	}

	/**
	 * 
	 * 批量硬更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	public <T extends Serializable> void hardUpdateBatch(Map<String, String> options, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			DML dml = UpdateDMLParser.getInstance().parse(rows.get(0).getClass());
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
		SQL sql = SQLDialectUtils.getSQLDialect(options).save(obj);
		return execute(options, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
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
	public <T extends Serializable> int save(Map<String, String> options, T obj, String... hardFields) {
		SQL sql = SQLDialectUtils.getSQLDialect(options).save(obj, hardFields);
		return execute(options, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int save(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(options, showSql, rows, SQLDialectUtils.getSQLDialect(options).save(rows.get(0).getClass()));
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
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int save(Map<String, String> options, List<T> rows, String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(options, showSql, rows,
				SQLDialectUtils.getSQLDialect(options).save(rows.get(0).getClass(), hardFields));
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
		saveBatch(options, rows, defaultBatchSize);
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
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows, String... hardFields) {
		saveBatch(options, rows, defaultBatchSize, hardFields);
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
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		saveBatch(options, rows, batchSize, SQLDialectUtils.getSQLDialect(options).save(rows.get(0).getClass()));
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
	public <T extends Serializable> void saveBatch(Map<String, String> options, List<T> rows, int batchSize,
			String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		saveBatch(options, rows, batchSize,
				SQLDialectUtils.getSQLDialect(options).save(rows.get(0).getClass(), hardFields));
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
		SQL sql = SQLDialectUtils.getSQLDialect(options).hardSave(obj);
		return execute(options, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	public <T extends Serializable> int hardSave(Map<String, String> options, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		SQLDialect dialect = SQLDialectUtils.getSQLDialect(options);
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			return JdbcUtils.hardSave(con, dialect, showSql, rows);
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

	/**
	 * 使用默认批容量批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param options
	 *            数据库配置
	 * @param rows
	 *            实体对象集
	 */
	public <T extends Serializable> void hardSaveBatch(Map<String, String> options, List<T> rows) {
		hardSaveBatch(options, rows, defaultBatchSize);
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
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = SQLDialectUtils.getSQLDialect(options);
		Connection con = null;
		PreparedStatement ps = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			MergeSQL mergeSql = dialect.hardSave(rows.get(0).getClass());
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					JdbcUtils.addBatch(ps, fieldMetas, rows.get(i));
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
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
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				JdbcUtils.close(ps);
			}
			JdbcUtils.close(con);
		}
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
				new GetSQLExecuter<T>(type));
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
			Map<String, ?> params) {
		return get(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 从数据库查询并组装实体对象列表
	 * 
	 * @param options
	 *            数据库配置
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象列表
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> List<T> select(Map<String, String> options, T obj) {
		Class<T> type = (Class<T>) obj.getClass();
		DML dml = GetDMLParser.getInstance().parse(type);
		return this.execute(options, dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()),
				new SelectSQLExecuter<T>(type));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
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
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
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
			Map<String, ?> params) {
		return select(options, sqltoolFactory.parse(dsql, params), type);
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
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
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	public boolean execute(Map<String, String> options, String dsql, Map<String, ?> params) {
		return this.execute(options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param options
	 *            数据库配置
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
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	public int executeUpdate(Map<String, String> options, String dsql, Map<String, ?> params) {
		return this.executeUpdate(options, sqltoolFactory.parse(dsql, params));
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
			transaction.execute(new SqltoolExecutor(showSql, sqltoolFactory, SQLDialectUtils.getSQLDialect(options)));
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
				sb.append("Execute SQL: ").append(sql).append(JdbcUtils.LINE_SEPARATOR).append("Params: ")
						.append(JSONUtils.toJSONString(params));
				log.info(sb.toString());
			}
			try {
				return JdbcUtils.execute(currentConnection.get(), sql, params, ExecuteUpdateSQLExecuter.getInstance(),
						showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 插入操作（实体对象集为空则直接返回null）
		 * 
		 * @param rows
		 *            实体对象集
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int insert(List<T> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return 0;
			} else {
				DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
				String sql = dml.getSql();
				if (showSql) {
					log.info("Execute SQL: ".concat(sql));
				}
				try {
					return JdbcUtils.insert(currentConnection.get(), showSql, rows);
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
			SQL sql = dialect.save(obj);
			try {
				return JdbcUtils.execute(currentConnection.get(), sql.getScript(), sql.getParams(),
						ExecuteUpdateSQLExecuter.getInstance(), showSql);
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
		public <T extends Serializable> int save(T obj, String... hardFields) {
			SQL sql = dialect.save(obj, hardFields);
			try {
				return JdbcUtils.execute(currentConnection.get(), sql.getScript(), sql.getParams(),
						ExecuteUpdateSQLExecuter.getInstance(), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		/**
		 * 软保存。仅对属性值不为null的字段执行插入/更新操作
		 * 
		 * @param rows
		 *            实体对象集
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int save(List<T> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return 0;
			}
			try {
				return JdbcUtils.save(currentConnection.get(), showSql, rows, dialect.save(rows.get(0).getClass()));
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
		 * @return 返回受影响行数
		 */
		public <T extends Serializable> int save(List<T> rows, String... hardFields) {
			if (CollectionUtils.isEmpty(rows)) {
				return 0;
			}
			try {
				return JdbcUtils.save(currentConnection.get(), showSql, rows,
						dialect.save(rows.get(0).getClass(), hardFields));
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
			SQL sql = dialect.hardSave(obj);
			try {
				return JdbcUtils.execute(currentConnection.get(), sql.getScript(), sql.getParams(),
						ExecuteUpdateSQLExecuter.getInstance(), showSql);
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
		public <T extends Serializable> int hardSave(List<T> rows) {
			if (CollectionUtils.isEmpty(rows)) {
				return 0;
			}
			try {
				return JdbcUtils.hardSave(currentConnection.get(), dialect, showSql, rows);
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
				return JdbcUtils.execute(currentConnection.get(), dml.getSql(),
						JdbcUtils.getParams(obj, dml.getFields()), new GetSQLExecuter<T>(type), showSql);
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
		public <T extends Serializable> T get(Class<T> type, String dsql, Map<String, ?> params) {
			return get(sqltoolFactory.parse(dsql, params), type);
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
		 */
		public <T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params) {
			return select(sqltoolFactory.parse(dsql, params), type);
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
		 */
		public <T extends Serializable> List<T> select(Class<T> type, String dsql, Map<String, ?> params) {
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
		public boolean execute(String dsql, Map<String, ?> params) {
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
		public int executeUpdate(String dsql, Map<String, ?> params) {
			return executeUpdate(sqltoolFactory.parse(dsql, params));
		}

		private boolean execute(NamedSQL namedSQL) {
			SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
			PreparedStatement ps = null;
			boolean rs = false;
			Connection con = currentConnection.get();
			try {
				ps = con.prepareStatement(sql.getScript());
				JdbcUtils.setParams(ps, sql.getParams());
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

		private int executeUpdate(NamedSQL namedSQL) {
			SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
			PreparedStatement ps = null;
			int count = 0;
			Connection con = currentConnection.get();
			try {
				String script = sql.getScript();
				List<Object> params = sql.getParams();
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

		private <T extends Serializable> T get(NamedSQL namedSQL, Class<T> type) {
			SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
			try {
				return JdbcUtils.execute(currentConnection.get(), sql.getScript(), sql.getParams(),
						new GetSQLExecuter<T>(type), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}

		private <T extends Serializable> List<T> select(NamedSQL namedSQL, Class<T> type) {
			SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
			try {
				return JdbcUtils.execute(currentConnection.get(), sql.getScript(), sql.getParams(),
						new SelectSQLExecuter<T>(type), showSql);
			} catch (SQLException e) {
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			}
		}
	}

	private <T extends Serializable> T get(Map<String, String> options, NamedSQL namedSQL, Class<T> type) {
		return this.execute(options, namedSQL, new GetSQLExecuter<T>(type));
	}

	private <T extends Serializable> List<T> select(Map<String, String> options, NamedSQL namedSQL, Class<T> type) {
		return this.execute(options, namedSQL, new SelectSQLExecuter<T>(type));
	}

	private boolean execute(Map<String, String> options, NamedSQL namedSQL) {
		return this.execute(options, namedSQL, ExecuteSQLExecuter.getInstance());
	}

	private int executeUpdate(Map<String, String> options, NamedSQL namedSQL) {
		return this.execute(options, namedSQL, ExecuteUpdateSQLExecuter.getInstance());
	}

	private <T> T execute(Map<String, String> options, NamedSQL namedSQL, SQLExecuter<T> sqlExecuter) {
		SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		return this.execute(options, sql.getScript(), sql.getParams(), sqlExecuter);
	}

	private <T> T execute(Map<String, String> options, String sql, List<Object> params, SQLExecuter<T> sqlExecuter) {
		Connection con = null;
		T result = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			result = JdbcUtils.execute(con, sql, params, sqlExecuter, showSql);
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return result;
	}

	private <T extends Serializable> void executeBatch(Map<String, String> options, String sql, List<T> rows,
			List<Field> fields, int batchSize) {
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
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					JdbcUtils.addBatch(ps, rows.get(i), fields);
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
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
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				JdbcUtils.close(ps);
			}
			JdbcUtils.close(con);
		}
	}

	private static <T> int update(Map<String, String> options, boolean showSql, List<T> rows, UpdateSQL updateSQL) {
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			return JdbcUtils.update(con, showSql, rows, updateSQL);
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

	private static <T> int save(Map<String, String> options, boolean showSql, List<T> rows, MergeSQL mergeSql) {
		Connection con = null;
		try {
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			return JdbcUtils.save(con, showSql, rows, mergeSql);
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

	private <T> void updateBatch(Map<String, String> options, List<T> rows, int batchSize, UpdateSQL updateSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			String sql = updateSql.getScript();
			List<Field> fields = updateSql.getFields();
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					JdbcUtils.addBatch(ps, rows.get(i), fields);
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
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
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				JdbcUtils.close(ps);
			}
			JdbcUtils.close(con);
		}
	}

	private <T> void saveBatch(Map<String, String> options, List<T> rows, int batchSize, MergeSQL mergeSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			Class.forName(options.get("driver"));
			con = DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
			con.setAutoCommit(false);
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					JdbcUtils.addBatch(ps, fieldMetas, rows.get(i));
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
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
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				JdbcUtils.close(ps);
			}
			JdbcUtils.close(con);
		}
	}

}
