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

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.Dao;
import cn.tenmg.sqltool.Transaction;
import cn.tenmg.sqltool.data.Page;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.dsql.utils.DSQLUtils;
import cn.tenmg.sqltool.exception.DetermineSQLDialectException;
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
import cn.tenmg.sqltool.sql.executer.LongResultSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.sql.utils.SQLUtils;
import cn.tenmg.sqltool.transaction.CurrentConnectionHolder;
import cn.tenmg.sqltool.transaction.TransactionExecutor;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

public abstract class AbstractDao implements Dao {

	private static final Logger log = Logger.getLogger(AbstractDao.class);

	protected static final Map<DataSource, SQLDialect> DIALECTS = new HashMap<DataSource, SQLDialect>();

	abstract boolean isShowSql();

	abstract int getDefaultBatchSize();

	protected SQLDialect getSQLDialect(DataSource dataSource) {
		SQLDialect dialect = DIALECTS.get(dataSource);
		if (dialect == null) {
			Connection con = null;
			try {
				con = dataSource.getConnection();
				con.setReadOnly(true);
				String url = con.getMetaData().getURL();
				dialect = SQLDialectUtils.getSQLDialect(url);
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
		DML dml = InsertDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return execute(dataSource, dml.getSql(), params, ExecuteUpdateSQLExecuter.getInstance());
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
			Connection con = null;
			try {
				con = dataSource.getConnection();
				con.setAutoCommit(false);
				con.setReadOnly(false);
				int count = JdbcUtils.insert(con, isShowSql(), rows);
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
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			executeBatch(dataSource, dml.getSql(), rows, dml.getFields(), batchSize);
		}
	}

	@Override
	public <T extends Serializable> int update(T obj) {
		return update(getDefaultDataSource(), obj);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, T obj) {
		SQL sql = getSQLDialect(dataSource).update(obj);
		return execute(dataSource, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int update(T obj, String... hardFields) {
		return update(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T extends Serializable> int update(DataSource dataSource, T obj, String... hardFields) {
		SQL sql = getSQLDialect(dataSource).update(obj, hardFields);
		return execute(dataSource, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
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
		DML dml = UpdateDMLParser.getInstance().parse(obj.getClass());
		List<Object> params = JdbcUtils.getParams(obj, dml.getFields());
		return execute(dataSource, dml.getSql(), params, ExecuteUpdateSQLExecuter.getInstance());
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
		return execute(dataSource, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T extends Serializable> int save(T obj, String... hardFields) {
		return save(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T extends Serializable> int save(DataSource dataSource, T obj, String... hardFields) {
		SQL sql = getSQLDialect(dataSource).save(obj, hardFields);
		return execute(dataSource, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
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
		return execute(dataSource, sql.getScript(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
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
			if (isShowSql()) {
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
	public <T extends Serializable> T get(T obj) {
		return get(getDefaultDataSource(), obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T get(DataSource dataSource, T obj) {
		Class<T> type = (Class<T>) obj.getClass();
		DML dml = GetDMLParser.getInstance().parse(type);
		return execute(dataSource, dml.getSql(), JdbcUtils.getParams(obj, dml.getFields()),
				new GetSQLExecuter<T>(type));
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
		return execute(dataSource, sql.getScript(), sql.getParams(), new SelectSQLExecuter<T>(type));
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

	private <T> T execute(DataSource dataSource, String sql, List<Object> params, SQLExecuter<T> sqlExecuter) {
		Connection con = null;
		T result = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(sqlExecuter.isReadOnly());
			result = JdbcUtils.execute(con, sql, params, sqlExecuter, isShowSql());
		} catch (SQLException e) {
			throw new cn.tenmg.sqltool.exception.SQLException(e);
		} finally {
			JdbcUtils.close(con);
		}
		return result;
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
			if (isShowSql()) {
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
			if (isShowSql()) {
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
			if (isShowSql()) {
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
		SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		return execute(dataSource, sql.getScript(), sql.getParams(), sqlExecuter);
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
		SQLDialect dialect = getSQLDialect(dataSource);
		SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		String script = sql.getScript();
		List<Object> params = sql.getParams();
		return page(dataSource, dialect, script, dialect.countSql(script), params, params, currentPage, pageSize, type);
	}

	private <T extends Serializable> Page<T> page(DataSource dataSource, NamedSQL namedSQL, NamedSQL countNamedSQL,
			long currentPage, int pageSize, Class<T> type) {
		SQLDialect dialect = getSQLDialect(dataSource);
		SQL sql = DSQLUtils.toSQL(namedSQL.getScript(), namedSQL.getParams());
		SQL countSql = DSQLUtils.toSQL(countNamedSQL.getScript(), countNamedSQL.getParams());
		String script = sql.getScript();
		return page(dataSource, dialect, script, dialect.countSql(script), sql.getParams(), countSql.getParams(),
				currentPage, pageSize, type);
	}

	private <T extends Serializable> Page<T> page(DataSource dataSource, SQLDialect dialect, String sql,
			String countSql, List<Object> params, List<Object> countParams, long currentPage, int pageSize,
			Class<T> type) {
		Connection con = null;
		Page<T> page = new Page<T>();
		page.setCurrentPage(currentPage);
		page.setPageSize(pageSize);
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(true);
			boolean showSql = isShowSql();
			Long total = JdbcUtils.execute(con, countSql, countParams, LongResultSQLExecuter.getInstance(), showSql);
			page.setTotal(total);
			if (total != null && total > 0) {
				page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
				page.setRows(JdbcUtils.execute(con, dialect.pageSql(sql, pageSize, currentPage), params,
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
