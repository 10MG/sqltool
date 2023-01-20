package cn.tenmg.sqltool.transaction;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import cn.tenmg.dsl.Script;
import cn.tenmg.dsl.utils.CollectionUtils;
import cn.tenmg.dsql.DSQLFactory;
import cn.tenmg.dsql.NamedSQL;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.executer.ExecuteSQLExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSQLExecuter;
import cn.tenmg.sqltool.sql.executer.GetSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.parser.DeleteDMLParser;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.sql.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * 事务执行器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.2.0
 */
public class TransactionExecutor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4415310449248911047L;

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
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				dml.getSql(), EntityUtils.getParams(obj, dml.getFields()), showSql);
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
			return JDBCExecuteUtils.executeBatch(CurrentConnectionHolder.get(), InsertDMLParser.getInstance(), rows,
					showSql);
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
		Script<List<Object>> sql = dialect.update(obj);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				sql.getValue(), sql.getParams(), showSql);
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
		Script<List<Object>> sql = dialect.update(obj, hardFields);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				sql.getValue(), sql.getParams(), showSql);
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
		return JDBCExecuteUtils.update(CurrentConnectionHolder.get(), dialect.update(rows.get(0).getClass()), rows,
				showSql);
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
		return JDBCExecuteUtils.update(CurrentConnectionHolder.get(),
				dialect.update(rows.get(0).getClass(), hardFields), rows, showSql);
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
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				dml.getSql(), EntityUtils.getParams(obj, dml.getFields()), showSql);

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
		return JDBCExecuteUtils.hardUpdate(CurrentConnectionHolder.get(), rows, showSql);
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
		Script<List<Object>> sql = dialect.save(obj);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				sql.getValue(), sql.getParams(), showSql);
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
		Script<List<Object>> sql = dialect.save(obj, hardFields);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				sql.getValue(), sql.getParams(), showSql);
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
		return JDBCExecuteUtils.save(CurrentConnectionHolder.get(), dialect.save(rows.get(0).getClass()), rows,
				showSql);
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
		return JDBCExecuteUtils.save(CurrentConnectionHolder.get(), dialect.save(rows.get(0).getClass(), hardFields),
				rows, showSql);
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
		Script<List<Object>> sql = dialect.hardSave(obj);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				sql.getValue(), sql.getParams(), showSql);
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
		return JDBCExecuteUtils.hardSave(CurrentConnectionHolder.get(), dialect, rows, showSql);
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
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(), null,
				dml.getSql(), EntityUtils.getParams(obj, dml.getFields()), showSql);
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
		return JDBCExecuteUtils.executeBatch(CurrentConnectionHolder.get(), DeleteDMLParser.getInstance(), rows,
				showSql);
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
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), new GetSQLExecuter<T>(type), null, dml.getSql(),
				EntityUtils.getParams(obj, dml.getFields()), showSql);
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
		Script<List<Object>> sql = DSQLFactory.toJDBC(namedSQL);
		return (boolean) JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteSQLExecuter.getInstance(),
				namedSQL.getId(), sql.getValue(), sql.getParams(), showSql);
	}

	private int executeUpdate(NamedSQL namedSQL) throws SQLException {
		Script<List<Object>> sql = DSQLFactory.toJDBC(namedSQL);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), ExecuteUpdateSQLExecuter.getInstance(),
				namedSQL.getId(), sql.getValue(), sql.getParams(), showSql);
	}

	private <T extends Serializable> T get(NamedSQL namedSQL, Class<T> type) throws SQLException {
		Script<List<Object>> sql = DSQLFactory.toJDBC(namedSQL);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), new GetSQLExecuter<T>(type), namedSQL.getId(),
				sql.getValue(), sql.getParams(), showSql);
	}

	private <T extends Serializable> List<T> select(NamedSQL namedSQL, Class<T> type) throws SQLException {
		Script<List<Object>> sql = DSQLFactory.toJDBC(namedSQL);
		return JDBCExecuteUtils.execute(CurrentConnectionHolder.get(), new SelectSQLExecuter<T>(type), namedSQL.getId(),
				sql.getValue(), sql.getParams(), showSql);
	}
}
