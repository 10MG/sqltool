package cn.tenmg.sqltool.dao;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.tenmg.sqltool.Dao;
import cn.tenmg.sqltool.Transaction;
import cn.tenmg.sqltool.data.Page;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.exception.DetermineSQLDialectException;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.TransactionException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.executer.ExecuteSQLExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSQLExecuter;
import cn.tenmg.sqltool.sql.executer.GetSQLExecuter;
import cn.tenmg.sqltool.sql.executer.LongResultSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.DeleteDMLParser;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.sql.utils.SQLUtils;
import cn.tenmg.sqltool.transaction.CurrentConnectionHolder;
import cn.tenmg.sqltool.transaction.TransactionExecutor;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

/**
 * 抽象数据库访问对象
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractDao implements Dao {

	private static final Logger log = LogManager.getLogger(AbstractDao.class);

	private static final Map<DataSource, SQLDialect> DIALECTS = new HashMap<DataSource, SQLDialect>();

	abstract boolean isShowSql();

	abstract int getDefaultBatchSize();

	protected static synchronized void cacheSQLDialect(DataSource dataSource, SQLDialect dialect) {
		DIALECTS.put(dataSource, dialect);
	}

	protected SQLDialect getSQLDialect(DataSource dataSource) {
		SQLDialect dialect = DIALECTS.get(dataSource);
		if (dialect == null) {
			Connection con = null;
			try {
				con = dataSource.getConnection();
				con.setReadOnly(true);
				String url = con.getMetaData().getURL();
				dialect = SQLDialectUtils.getSQLDialect(url);
				cacheSQLDialect(dataSource, dialect);
			} catch (SQLException e) {
				throw new DetermineSQLDialectException("SQLException occured while getting url of the dataSource", e);
			} finally {
				JdbcUtils.close(con);
			}
		}
		return dialect;
	}

	@Override
	public <T extends Serializable> int insert(T obj) {
		return insert(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int insert(DataSource dataSource, T obj) {
		return execute(dataSource, obj, InsertDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int insert(List<T> rows) {
		return insert(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int insert(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			return execute(dataSource, rows, InsertDMLParser.getInstance());
		}
	}

	@Override
	public <T extends Serializable> void insertBatch(List<T> rows) {
		insertBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void insertBatch(DataSource dataSource, List<T> rows) {
		insertBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void insertBatch(List<T> rows, int batchSize) {
		insertBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T extends Serializable> void insertBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			executeBatch(dataSource, rows, InsertDMLParser.getInstance(), batchSize);
		}
	}

	@Override
	public <T extends Serializable> int update(T obj) {
		return update(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, T obj) {
		SQL sql = getSQLDialect(dataSource).update(obj);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int update(T obj, String... hardFields) {
		return update(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, T obj, String... hardFields) {
		SQL sql = getSQLDialect(dataSource).update(obj, hardFields);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int update(List<T> rows) {
		return update(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return update(dataSource, isShowSql(), rows, getSQLDialect(dataSource).update(rows.get(0).getClass()));
	}

	@Override
	public <T extends Serializable> int update(List<T> rows, String... hardFields) {
		return update(getDefaultDataSource(), rows, hardFields);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, List<T> rows, String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return update(dataSource, isShowSql(), rows,
				getSQLDialect(dataSource).update(rows.get(0).getClass(), hardFields));
	}

	@Override
	public <T extends Serializable> void updateBatch(List<T> rows) {
		updateBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows) {
		updateBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void updateBatch(List<T> rows, String... hardFields) {
		updateBatch(rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, String... hardFields) {
		updateBatch(dataSource, rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T extends Serializable> void updateBatch(List<T> rows, int batchSize) {
		updateBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		updateBatch(dataSource, rows, batchSize, getSQLDialect(dataSource).update(rows.get(0).getClass()));
	}

	@Override
	public <T extends Serializable> void updateBatch(List<T> rows, int batchSize, String... hardFields) {
		updateBatch(getDefaultDataSource(), batchSize, rows, hardFields);
	}

	@Override
	public <T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, int batchSize,
			String... hardFields) {
		updateBatch(dataSource, batchSize, rows, hardFields);
	}

	@Override
	public <T extends Serializable> int hardUpdate(T obj) {
		return hardUpdate(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int hardUpdate(DataSource dataSource, T obj) {
		return execute(dataSource, obj, UpdateDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int hardUpdate(List<T> rows) {
		return hardUpdate(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int hardUpdate(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			Connection con = null;
			try {
				con = dataSource.getConnection();
				con.setAutoCommit(false);
				con.setReadOnly(false);
				int count = JdbcUtils.hardUpdate(con, isShowSql(), rows);
				con.commit();
				return count;
			} catch (SQLException e) {
				try {
					con.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				throw new cn.tenmg.sqltool.exception.SQLException(e);
			} finally {
				JdbcUtils.close(con);
			}
		}
	}

	@Override
	public <T extends Serializable> void hardUpdateBatch(List<T> rows) {
		hardUpdateBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void hardUpdateBatch(DataSource dataSource, List<T> rows) {
		hardUpdateBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void hardUpdateBatch(List<T> rows, int batchSize) {
		hardUpdateBatch(getDefaultDataSource(), rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void hardUpdateBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			DML dml = UpdateDMLParser.getInstance().parse(rows.get(0).getClass());
			executeBatch(dataSource, dml.getSql(), rows, dml.getFields(), batchSize);
		}
	}

	@Override
	public <T extends Serializable> int save(T obj) {
		return save(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int save(DataSource dataSource, T obj) {
		SQL sql = getSQLDialect(dataSource).save(obj);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int save(T obj, String... hardFields) {
		return save(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T extends Serializable> int save(DataSource dataSource, T obj, String... hardFields) {
		SQL sql = getSQLDialect(dataSource).save(obj, hardFields);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int save(List<T> rows) {
		return save(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int save(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(dataSource, isShowSql(), rows, getSQLDialect(dataSource).save(rows.get(0).getClass()));
	}

	@Override
	public <T extends Serializable> int save(List<T> rows, String... hardFields) {
		return save(getDefaultDataSource(), rows, hardFields);
	}

	@Override
	public <T extends Serializable> int save(DataSource dataSource, List<T> rows, String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(dataSource, isShowSql(), rows, getSQLDialect(dataSource).save(rows.get(0).getClass(), hardFields));
	}

	@Override
	public <T extends Serializable> void saveBatch(List<T> rows) {
		saveBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows) {
		saveBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void saveBatch(List<T> rows, String... hardFields) {
		saveBatch(rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, String... hardFields) {
		saveBatch(dataSource, rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T extends Serializable> void saveBatch(List<T> rows, int batchSize) {
		saveBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		saveBatch(dataSource, rows, batchSize, getSQLDialect(dataSource).save(rows.get(0).getClass()));
	}

	@Override
	public <T extends Serializable> void saveBatch(List<T> rows, int batchSize, String... hardFields) {
		saveBatch(getDefaultDataSource(), rows, batchSize, hardFields);
	}

	@Override
	public <T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, int batchSize,
			String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		saveBatch(dataSource, rows, batchSize, getSQLDialect(dataSource).save(rows.get(0).getClass(), hardFields));
	}

	@Override
	public <T extends Serializable> int hardSave(T obj) {
		return hardSave(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int hardSave(DataSource dataSource, T obj) {
		SQL sql = getSQLDialect(dataSource).hardSave(obj);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int hardSave(List<T> rows) {
		return hardSave(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int hardSave(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		SQLDialect dialect = getSQLDialect(dataSource);
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JdbcUtils.hardSave(con, dialect, isShowSql(), rows);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	@Override
	public <T extends Serializable> void hardSaveBatch(List<T> rows) {
		hardSaveBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void hardSaveBatch(DataSource dataSource, List<T> rows) {
		hardSaveBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void hardSaveBatch(List<T> rows, int batchSize) {
		hardSaveBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T extends Serializable> void hardSaveBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		SQLDialect dialect = getSQLDialect(dataSource);
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			MergeSQL mergeSql = dialect.hardSave(rows.get(0).getClass());
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (isShowSql() && log.isInfoEnabled()) {
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

	@Override
	public <T extends Serializable> int delete(T obj) {
		return delete(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int delete(DataSource dataSource, T obj) {
		return execute(dataSource, obj, DeleteDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int delete(List<T> rows) {
		return delete(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> int delete(DataSource dataSource, List<T> rows) {
		return execute(dataSource, rows, DeleteDMLParser.getInstance());
	}

	@Override
	public <T extends Serializable> void deleteBatch(List<T> rows) {
		deleteBatch(getDefaultDataSource(), rows);
	}

	@Override
	public <T extends Serializable> void deleteBatch(DataSource dataSource, List<T> rows) {
		deleteBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T extends Serializable> void deleteBatch(List<T> rows, int batchSize) {
		deleteBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T extends Serializable> void deleteBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			executeBatch(dataSource, rows, DeleteDMLParser.getInstance(), batchSize);
		}
	}

	@Override
	public <T extends Serializable> T get(T obj) {
		return get(getDefaultDataSource(), obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(DataSource dataSource, T obj) {
		return execute(dataSource, obj, GetDMLParser.getInstance(), new GetSQLExecuter<T>((Class<T>) obj.getClass()));
	}

	@Override
	public <T extends Serializable> T get(Class<T> type, String dsql, Object... params) {
		return get(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> T get(DataSource dataSource, Class<T> type, String dsql, Object... params) {
		return get(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> T get(Class<T> type, String dsql, Map<String, ?> params) {
		return get(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> T get(DataSource dataSource, Class<T> type, String dsql, Map<String, ?> params) {
		return get(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> List<T> select(T obj) {
		return select(getDefaultDataSource(), obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> List<T> select(DataSource dataSource, T obj) {
		Class<T> type = (Class<T>) obj.getClass();
		SQL sql = SQLUtils.parseSelect(obj);
		return execute(dataSource, null, sql.getScript(), sql.getParams(), new SelectSQLExecuter<T>(type));
	}

	@Override
	public <T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params) {
		return select(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> List<T> select(DataSource dataSource, Class<T> type, String dsql,
			Object... params) {
		return select(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> List<T> select(Class<T> type, String dsql, Map<String, ?> params) {
		return select(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> List<T> select(DataSource dataSource, Class<T> type, String dsql,
			Map<String, ?> params) {
		return select(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T extends Serializable> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize,
			Object... params) {
		return page(getDefaultDataSource(), type, dsql, currentPage, pageSize, params);
	}

	@Override
	public <T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Object... params) {
		return page(dataSource, parse(dsql, params), currentPage, pageSize, type);
	}

	@Override
	public <T extends Serializable> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage,
			int pageSize, Object... params) {
		return page(getDefaultDataSource(), type, dsql, cntDsql, currentPage, pageSize, params);
	}

	@Override
	public <T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Object... params) {
		return page(dataSource, parse(dsql, params), parse(cntDsql, params), currentPage, pageSize, type);
	}

	@Override
	public <T extends Serializable> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize,
			Map<String, Object> params) {
		return page(getDefaultDataSource(), type, dsql, currentPage, pageSize, params);
	}

	@Override
	public <T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Map<String, Object> params) {
		return page(dataSource, parse(dsql, params), currentPage, pageSize, type);
	}

	@Override
	public <T extends Serializable> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage,
			int pageSize, Map<String, Object> params) {
		return page(getDefaultDataSource(), type, dsql, cntDsql, currentPage, pageSize, params);
	}

	@Override
	public <T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Map<String, Object> params) {
		return page(dataSource, parse(dsql, params), parse(cntDsql, params), currentPage, pageSize, type);
	}

	@Override
	public boolean execute(String dsql, Object... params) {
		return execute(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public boolean execute(DataSource dataSource, String dsql, Object... params) {
		return execute(dataSource, parse(dsql, params));
	}

	@Override
	public boolean execute(String dsql, Map<String, ?> params) {
		return execute(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public boolean execute(DataSource dataSource, String dsql, Map<String, ?> params) {
		return execute(dataSource, parse(dsql, params));
	}

	@Override
	public int executeUpdate(String dsql, Object... params) {
		return executeUpdate(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public int executeUpdate(DataSource dataSource, String dsql, Object... params) {
		return executeUpdate(dataSource, parse(dsql, params));
	}

	@Override
	public int executeUpdate(String dsql, Map<String, ?> params) {
		return executeUpdate(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public int executeUpdate(DataSource dataSource, String dsql, Map<String, ?> params) {
		return executeUpdate(dataSource, parse(dsql, params));
	}

	@Override
	public void execute(Transaction transaction) {
		execute(getDefaultDataSource(), transaction);
	}

	@Override
	public void execute(DataSource dataSource, Transaction transaction) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			CurrentConnectionHolder.set(con);
			transaction.execute(new TransactionExecutor(isShowSql(), getDSQLFactory(), getSQLDialect(dataSource)));
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
			CurrentConnectionHolder.remove();
		}
	}

	private NamedSQL parse(String dsql, Object... params) {
		return getDSQLFactory().parse(dsql, params);
	}

	private NamedSQL parse(String dsql, Map<String, ?> params) {
		return getDSQLFactory().parse(dsql, params);
	}

	private <T> T execute(DataSource dataSource, Object obj, DMLParser dmlParser, SQLExecuter<T> sqlExecuter) {
		DML dml = dmlParser.parse(obj.getClass());
		return execute(dataSource, null, dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()), sqlExecuter);
	}

	private <T> T execute(DataSource dataSource, String id, String sql, List<Object> params,
			SQLExecuter<T> sqlExecuter) {
		Connection con = null;
		T result = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(sqlExecuter.isReadOnly());
			result = JdbcUtils.execute(con, id, sql, params, sqlExecuter, isShowSql());
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return result;
	}

	private <T extends Serializable> int execute(DataSource dataSource, List<T> rows, DMLParser dmlParser) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JdbcUtils.executeBatch(con, isShowSql(), rows, dmlParser);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T extends Serializable> void executeBatch(DataSource dataSource, List<T> rows, DMLParser dmlParser,
			int batchSize) {
		DML dml = dmlParser.parse(rows.get(0).getClass());
		executeBatch(dataSource, dml.getSql(), rows, dml.getFields(), batchSize);
	}

	private <T extends Serializable> void executeBatch(DataSource dataSource, String sql, List<T> rows,
			List<Field> fields, int batchSize) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			ps = con.prepareStatement(sql);
			if (isShowSql() && log.isInfoEnabled()) {
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

	private static <T> int update(DataSource dataSource, boolean showSql, List<T> rows, UpdateSQL updateSQL) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JdbcUtils.update(con, showSql, rows, updateSQL);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T extends Serializable> void updateBatch(DataSource dataSource, int batchSize, List<T> rows,
			String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		updateBatch(dataSource, rows, batchSize, getSQLDialect(dataSource).update(rows.get(0).getClass(), hardFields));
	}

	private <T> void updateBatch(DataSource dataSource, List<T> rows, int batchSize, UpdateSQL updateSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			String sql = updateSql.getScript();
			List<Field> fields = updateSql.getFields();
			if (isShowSql() && log.isInfoEnabled()) {
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

	private static <T> int save(DataSource dataSource, boolean showSql, List<T> rows, MergeSQL mergeSql) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JdbcUtils.save(con, showSql, rows, mergeSql);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
	}

	private <T> void saveBatch(DataSource dataSource, List<T> rows, int batchSize, MergeSQL mergeSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (isShowSql() && log.isInfoEnabled()) {
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

	private <T extends Serializable> T get(DataSource dataSource, NamedSQL namedSQL, Class<T> type) {
		return execute(dataSource, namedSQL, new GetSQLExecuter<T>(type));
	}

	private <T> T execute(DataSource dataSource, NamedSQL namedSQL, SQLExecuter<T> sqlExecuter) {
		SQL sql = SQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		return execute(dataSource, namedSQL.getId(), sql.getScript(), sql.getParams(), sqlExecuter);
	}

	private <T extends Serializable> List<T> select(DataSource dataSource, NamedSQL namedSQL, Class<T> type) {
		return execute(dataSource, namedSQL, new SelectSQLExecuter<T>(type));
	}

	private boolean execute(DataSource dataSource, NamedSQL namedSQL) {
		return execute(dataSource, namedSQL, ExecuteSQLExecuter.getInstance());
	}

	private int executeUpdate(DataSource dataSource, NamedSQL namedSQL) {
		return execute(dataSource, namedSQL, ExecuteUpdateSQLExecuter.getInstance());
	}

	private <T extends Serializable> Page<T> page(DataSource dataSource, NamedSQL namedSQL, long currentPage,
			int pageSize, Class<T> type) {
		Connection con = null;
		Page<T> page = new Page<T>();
		page.setCurrentPage(currentPage);
		page.setPageSize(pageSize);
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(true);
			boolean showSql = isShowSql();
			SQLDialect dialect = getSQLDialect(dataSource);
			String id = namedSQL.getId(), script = namedSQL.getScript();
			Map<String, Object> params = namedSQL.getParams();
			SQLMetaData sqlMetaData = SQLUtils.getSQLMetaData(script);
			SQL SQL = SQLUtils.toSQL(dialect.countSql(script, sqlMetaData), params);
			Long total = JdbcUtils.execute(con, id, SQL.getScript(), SQL.getParams(),
					LongResultSQLExecuter.getInstance(), showSql);
			page.setTotal(total);
			if (total != null && total > 0) {
				page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
				SQL = SQLUtils.toSQL(dialect.pageSql(con, script, params, sqlMetaData, pageSize, currentPage), params);
				page.setRows(JdbcUtils.execute(con, id, SQL.getScript(), SQL.getParams(),
						new SelectSQLExecuter<T>(type), showSql));
			} else {
				page.setTotalPage(0L);
			}
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return page;
	}

	private <T extends Serializable> Page<T> page(DataSource dataSource, NamedSQL namedSQL, NamedSQL countNamedSQL,
			long currentPage, int pageSize, Class<T> type) {
		Connection con = null;
		Page<T> page = new Page<T>();
		page.setCurrentPage(currentPage);
		page.setPageSize(pageSize);
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(true);
			boolean showSql = isShowSql();
			String script = countNamedSQL.getScript();
			SQLDialect dialect = getSQLDialect(dataSource);
			SQL SQL = SQLUtils.toSQL(dialect.countSql(script, SQLUtils.getSQLMetaData(script)),
					countNamedSQL.getParams());
			Long total = JdbcUtils.execute(con, countNamedSQL.getId(), SQL.getScript(), SQL.getParams(),
					LongResultSQLExecuter.getInstance(), showSql);
			page.setTotal(total);
			if (total != null && total > 0) {
				page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
				script = namedSQL.getScript();
				SQL = SQLUtils.toSQL(dialect.pageSql(con, script, namedSQL.getParams(), SQLUtils.getSQLMetaData(script),
						pageSize, currentPage), namedSQL.getParams());
				page.setRows(JdbcUtils.execute(con, namedSQL.getId(), SQL.getScript(), SQL.getParams(),
						new SelectSQLExecuter<T>(type), showSql));
			} else {
				page.setTotalPage(0L);
			}
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return page;
	}

}
