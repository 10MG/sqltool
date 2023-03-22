package cn.tenmg.sqltool.dao;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.tenmg.dsl.Script;
import cn.tenmg.dsl.utils.CollectionUtils;
import cn.tenmg.dsql.NamedSQL;
import cn.tenmg.sql.paging.SQLMetaData;
import cn.tenmg.sql.paging.utils.JDBCUtils;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.Dao;
import cn.tenmg.sqltool.Transaction;
import cn.tenmg.sqltool.data.Page;
import cn.tenmg.sqltool.exception.DetermineSQLDialectException;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.SQLExecutorException;
import cn.tenmg.sqltool.exception.TransactionException;
import cn.tenmg.sqltool.macro.Paging;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.executer.ExecuteSQLExecuter;
import cn.tenmg.sqltool.sql.executer.ExecuteUpdateSQLExecuter;
import cn.tenmg.sqltool.sql.executer.GetSQLExecuter;
import cn.tenmg.sqltool.sql.executer.LongResultSQLExecuter;
import cn.tenmg.sqltool.sql.executer.SelectSQLExecuter;
import cn.tenmg.sqltool.sql.parser.DeleteDMLParser;
import cn.tenmg.sqltool.sql.parser.GetDMLParser;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;
import cn.tenmg.sqltool.sql.utils.EntityUtils;
import cn.tenmg.sqltool.transaction.CurrentConnectionHolder;
import cn.tenmg.sqltool.transaction.TransactionExecutor;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

/**
 * 抽象数据库访问对象
 * 
 * @author June wjzhao@aliyun.com
 *
 */
public abstract class AbstractDao implements Dao {

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
				JDBCUtils.close(con);
			}
		}
		return dialect;
	}

	protected void closeDataSourcesWhenShutdown(Collection<DataSource> dataSources) {
		Runtime.getRuntime().addShutdownHook(new Thread("sqltool") {
			@Override
			public void run() {
				if (dataSources != null) {
					DataSource dataSource;
					for (Iterator<DataSource> it = dataSources.iterator(); it.hasNext();) {
						dataSource = it.next();
						if (dataSource instanceof Closeable) {
							try {
								((Closeable) dataSource).close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					DataSource defaultDataSource = getDefaultDataSource();
					if (!dataSources.contains(defaultDataSource)) {
						if (defaultDataSource instanceof Closeable) {
							try {
								((Closeable) defaultDataSource).close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}

	@Override
	public <T> int insert(T obj) {
		return insert(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int insert(DataSource dataSource, T obj) {
		return execute(dataSource, obj, InsertDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int insert(List<T> rows) {
		return insert(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int insert(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			return execute(dataSource, rows, InsertDMLParser.getInstance());
		}
	}

	@Override
	public <T> void insertBatch(List<T> rows) {
		insertBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T> void insertBatch(DataSource dataSource, List<T> rows) {
		insertBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void insertBatch(List<T> rows, int batchSize) {
		insertBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T> void insertBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			executeBatch(dataSource, rows, InsertDMLParser.getInstance(), batchSize);
		}
	}

	@Override
	public <T> int update(T obj) {
		return update(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int update(DataSource dataSource, T obj) {
		Script<List<Object>> sql = getSQLDialect(dataSource).update(obj);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int update(T obj, String... hardFields) {
		return update(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T> int update(DataSource dataSource, T obj, String... hardFields) {
		Script<List<Object>> sql = getSQLDialect(dataSource).update(obj, hardFields);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int update(List<T> rows) {
		return update(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int update(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return update(dataSource, isShowSql(), rows, getSQLDialect(dataSource).update(rows.get(0).getClass()));
	}

	@Override
	public <T> int update(List<T> rows, String... hardFields) {
		return update(getDefaultDataSource(), rows, hardFields);
	}

	@Override
	public <T> int update(DataSource dataSource, List<T> rows, String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return update(dataSource, isShowSql(), rows,
				getSQLDialect(dataSource).update(rows.get(0).getClass(), hardFields));
	}

	@Override
	public <T> void updateBatch(List<T> rows) {
		updateBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T> void updateBatch(DataSource dataSource, List<T> rows) {
		updateBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void updateBatch(List<T> rows, String... hardFields) {
		updateBatch(rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T> void updateBatch(DataSource dataSource, List<T> rows, String... hardFields) {
		updateBatch(dataSource, rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T> void updateBatch(List<T> rows, int batchSize) {
		updateBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T> void updateBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		JDBCExecuteUtils.updateBatch(dataSource, getSQLDialect(dataSource).update(rows.get(0).getClass()), rows,
				batchSize, isShowSql());
	}

	@Override
	public <T> void updateBatch(List<T> rows, int batchSize, String... hardFields) {
		updateBatch(getDefaultDataSource(), batchSize, rows, hardFields);
	}

	@Override
	public <T> void updateBatch(DataSource dataSource, List<T> rows, int batchSize,
			String... hardFields) {
		updateBatch(dataSource, batchSize, rows, hardFields);
	}

	@Override
	public <T> int hardUpdate(T obj) {
		return hardUpdate(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int hardUpdate(DataSource dataSource, T obj) {
		return execute(dataSource, obj, UpdateDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int hardUpdate(List<T> rows) {
		return hardUpdate(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int hardUpdate(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		} else {
			Connection con = null;
			try {
				con = dataSource.getConnection();
				con.setAutoCommit(false);
				con.setReadOnly(false);
				int count = JDBCExecuteUtils.hardUpdate(con, rows, isShowSql());
				con.commit();
				return count;
			} catch (SQLException e) {
				try {
					con.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
				throw new SQLExecutorException(e);
			} finally {
				JDBCUtils.close(con);
			}
		}
	}

	@Override
	public <T> void hardUpdateBatch(List<T> rows) {
		hardUpdateBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T> void hardUpdateBatch(DataSource dataSource, List<T> rows) {
		hardUpdateBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void hardUpdateBatch(List<T> rows, int batchSize) {
		hardUpdateBatch(getDefaultDataSource(), rows, getDefaultBatchSize());
	}

	@Override
	public <T> void hardUpdateBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			DML dml = UpdateDMLParser.getInstance().parse(rows.get(0).getClass());
			JDBCExecuteUtils.executeBatch(dataSource, dml.getSql(), rows, dml.getFields(), batchSize, isShowSql());
		}
	}

	@Override
	public <T> int save(T obj) {
		return save(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int save(DataSource dataSource, T obj) {
		Script<List<Object>> sql = getSQLDialect(dataSource).save(obj);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int save(T obj, String... hardFields) {
		return save(getDefaultDataSource(), obj, hardFields);
	}

	@Override
	public <T> int save(DataSource dataSource, T obj, String... hardFields) {
		Script<List<Object>> sql = getSQLDialect(dataSource).save(obj, hardFields);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int save(List<T> rows) {
		return save(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int save(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(dataSource, isShowSql(), rows, getSQLDialect(dataSource).save(rows.get(0).getClass()));
	}

	@Override
	public <T> int save(List<T> rows, String... hardFields) {
		return save(getDefaultDataSource(), rows, hardFields);
	}

	@Override
	public <T> int save(DataSource dataSource, List<T> rows, String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		return save(dataSource, isShowSql(), rows, getSQLDialect(dataSource).save(rows.get(0).getClass(), hardFields));
	}

	@Override
	public <T> void saveBatch(List<T> rows) {
		saveBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T> void saveBatch(DataSource dataSource, List<T> rows) {
		saveBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void saveBatch(List<T> rows, String... hardFields) {
		saveBatch(rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T> void saveBatch(DataSource dataSource, List<T> rows, String... hardFields) {
		saveBatch(dataSource, rows, getDefaultBatchSize(), hardFields);
	}

	@Override
	public <T> void saveBatch(List<T> rows, int batchSize) {
		saveBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T> void saveBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		JDBCExecuteUtils.saveBatch(dataSource, getSQLDialect(dataSource).save(rows.get(0).getClass()), rows, batchSize,
				isShowSql());
	}

	@Override
	public <T> void saveBatch(List<T> rows, int batchSize, String... hardFields) {
		saveBatch(getDefaultDataSource(), rows, batchSize, hardFields);
	}

	@Override
	public <T> void saveBatch(DataSource dataSource, List<T> rows, int batchSize,
			String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		JDBCExecuteUtils.saveBatch(dataSource, getSQLDialect(dataSource).save(rows.get(0).getClass(), hardFields), rows,
				batchSize, isShowSql());
	}

	@Override
	public <T> int hardSave(T obj) {
		return hardSave(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int hardSave(DataSource dataSource, T obj) {
		Script<List<Object>> sql = getSQLDialect(dataSource).hardSave(obj);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int hardSave(List<T> rows) {
		return hardSave(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int hardSave(DataSource dataSource, List<T> rows) {
		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}
		SQLDialect dialect = getSQLDialect(dataSource);
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JDBCExecuteUtils.hardSave(con, dialect, rows, isShowSql());
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.close(con);
		}
	}

	@Override
	public <T> void hardSaveBatch(List<T> rows) {
		hardSaveBatch(rows, getDefaultBatchSize());
	}

	@Override
	public <T> void hardSaveBatch(DataSource dataSource, List<T> rows) {
		hardSaveBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void hardSaveBatch(List<T> rows, int batchSize) {
		hardSaveBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T> void hardSaveBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		JDBCExecuteUtils.saveBatch(dataSource, getSQLDialect(dataSource).hardSave(rows.get(0).getClass()), rows,
				batchSize, isShowSql());
	}

	@Override
	public <T> int delete(T obj) {
		return delete(getDefaultDataSource(), obj);
	}

	@Override
	public <T> int delete(DataSource dataSource, T obj) {
		return execute(dataSource, obj, DeleteDMLParser.getInstance(), ExecuteUpdateSQLExecuter.getInstance());
	}

	@Override
	public <T> int delete(List<T> rows) {
		return delete(getDefaultDataSource(), rows);
	}

	@Override
	public <T> int delete(DataSource dataSource, List<T> rows) {
		return execute(dataSource, rows, DeleteDMLParser.getInstance());
	}

	@Override
	public <T> void deleteBatch(List<T> rows) {
		deleteBatch(getDefaultDataSource(), rows);
	}

	@Override
	public <T> void deleteBatch(DataSource dataSource, List<T> rows) {
		deleteBatch(dataSource, rows, getDefaultBatchSize());
	}

	@Override
	public <T> void deleteBatch(List<T> rows, int batchSize) {
		deleteBatch(getDefaultDataSource(), rows, batchSize);
	}

	@Override
	public <T> void deleteBatch(DataSource dataSource, List<T> rows, int batchSize) {
		if (!CollectionUtils.isEmpty(rows)) {
			executeBatch(dataSource, rows, DeleteDMLParser.getInstance(), batchSize);
		}
	}

	@Override
	public <T> T get(T obj) {
		return get(getDefaultDataSource(), obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(DataSource dataSource, T obj) {
		return execute(dataSource, obj, GetDMLParser.getInstance(), new GetSQLExecuter<T>((Class<T>) obj.getClass()));
	}

	@Override
	public <T> T get(Class<T> type, String dsql, Object... params) {
		return get(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T> T get(DataSource dataSource, Class<T> type, String dsql, Object... params) {
		return get(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T> T get(Class<T> type, String dsql, Object params) {
		return get(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T> T get(DataSource dataSource, Class<T> type, String dsql, Object params) {
		return get(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T> List<T> select(T obj) {
		return select(getDefaultDataSource(), obj);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> select(DataSource dataSource, T obj) {
		Class<T> type = (Class<T>) obj.getClass();
		Script<List<Object>> sql = EntityUtils.parseSelect(obj);
		return execute(dataSource, null, sql.getValue(), sql.getParams(), new SelectSQLExecuter<T>(type));
	}

	@Override
	public <T> List<T> select(Class<T> type, String dsql, Object... params) {
		return select(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T> List<T> select(DataSource dataSource, Class<T> type, String dsql,
			Object... params) {
		return select(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T> List<T> select(Class<T> type, String dsql, Object params) {
		return select(getDefaultDataSource(), parse(dsql, params), type);
	}

	@Override
	public <T> List<T> select(DataSource dataSource, Class<T> type, String dsql, Object params) {
		return select(dataSource, parse(dsql, params), type);
	}

	@Override
	public <T> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize,
			Object... params) {
		return page(getDefaultDataSource(), type, dsql, currentPage, pageSize, params);
	}

	@Override
	public <T> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Object... params) {
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
			Paging.initCountEnv(dialect);// 初始化Paging的计数查询SQL解析环境
			NamedSQL namedSQL = parse(dsql, params);// 假设存在#[page(……)]，尝试解析COUNT查询SQL
			Script<List<Object>> sql;
			String id = namedSQL.getId();
			SQLMetaData sqlMetaData;
			if (Paging.isCounted()) {// 已被Paging解析为COUNT查询SQL
				sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
				Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), id, sql.getValue(),
						sql.getParams(), showSql);
				page.setTotal(total);
				if (total != null && total > 0) {
					page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
					Paging.initPageEnv(dialect, con, page);// 初始化Paging的分页查询SQL解析环境
					namedSQL = parse(dsql, params);// 解析分页查询SQL
					sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
					page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), id, sql.getValue(),
							sql.getParams(), showSql));
				} else {
					page.setTotalPage(0L);
				}
			} else {// 重新按普通场景解析
				namedSQL = parse(dsql, params);
				String script = namedSQL.getScript();
				sqlMetaData = SQLUtils.getSQLMetaData(script);
				Map<String, Object> usedParams = namedSQL.getParams();
				sql = toJDBC(dialect.countSql(script, sqlMetaData), usedParams);
				Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), id, sql.getValue(),
						sql.getParams(), showSql);
				page.setTotal(total);
				if (total != null && total > 0) {
					page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
					sql = toJDBC(dialect.pageSql(con, script, usedParams, sqlMetaData, pageSize, currentPage),
							usedParams);
					page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), id, sql.getValue(),
							sql.getParams(), showSql));
				} else {
					page.setTotalPage(0L);
				}
			}
		} catch (SQLException e) {
			throw new SQLExecutorException(e);
		} finally {
			Paging.clear();
			JDBCUtils.close(con);
		}
		return page;
	}

	@Override
	public <T> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage,
			int pageSize, Object... params) {
		return page(getDefaultDataSource(), type, dsql, cntDsql, currentPage, pageSize, params);
	}

	@Override
	public <T> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Object... params) {
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
			Paging.initCountEnv(dialect);// 初始化Paging的计数查询SQL解析环境
			NamedSQL countNamedSQL = parse(cntDsql, params);
			Script<List<Object>> sql;
			if (Paging.isCounted()) {
				sql = toJDBC(countNamedSQL.getScript(), countNamedSQL.getParams());
			} else {
				String script = countNamedSQL.getScript();
				sql = toJDBC(dialect.countSql(script, SQLUtils.getSQLMetaData(script)), countNamedSQL.getParams());
			}
			Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), countNamedSQL.getId(),
					sql.getValue(), sql.getParams(), showSql);
			page.setTotal(total);
			if (total != null && total > 0) {
				page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
				Paging.initPageEnv(dialect, con, page);// 初始化Paging的分页查询SQL解析环境
				NamedSQL namedSQL = parse(dsql, params);// 假设存在#[page(……)]，尝试解析分页查询SQL
				if (Paging.isPaged()) {// DSL已被Paging解析为分页查询SQL
					sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
				} else {
					String script = namedSQL.getScript();
					sql = toJDBC(dialect.pageSql(con, script, namedSQL.getParams(), SQLUtils.getSQLMetaData(script),
							pageSize, currentPage), namedSQL.getParams());
				}
				page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), namedSQL.getId(),
						sql.getValue(), sql.getParams(), showSql));
			} else {
				page.setTotalPage(0L);
			}
		} catch (SQLException e) {
			throw new SQLExecutorException(e);
		} finally {
			Paging.clear();
			JDBCUtils.close(con);
		}
		return page;
	}

	@Override
	public <T> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize,
			Object params) {
		return page(getDefaultDataSource(), type, dsql, currentPage, pageSize, params);
	}

	@Override
	public <T> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Object params) {
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
			Paging.initCountEnv(dialect);// 初始化Paging的计数查询SQL解析环境
			NamedSQL namedSQL = parse(dsql, params);// 假设存在#[page(……)]，尝试解析COUNT查询SQL
			Script<List<Object>> sql;
			String id = namedSQL.getId();
			SQLMetaData sqlMetaData;
			if (Paging.isCounted()) {// 已被Paging解析为COUNT查询SQL
				sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
				Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), id, sql.getValue(),
						sql.getParams(), showSql);
				page.setTotal(total);
				if (total != null && total > 0) {
					page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
					Paging.initPageEnv(dialect, con, page);// 初始化Paging的分页查询SQL解析环境
					namedSQL = parse(dsql, params);// 解析分页查询SQL
					sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
					page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), id, sql.getValue(),
							sql.getParams(), showSql));
				} else {
					page.setTotalPage(0L);
				}
			} else {// 重新按普通场景解析
				namedSQL = parse(dsql, params);
				String script = namedSQL.getScript();
				sqlMetaData = SQLUtils.getSQLMetaData(script);
				Map<String, Object> usedParams = namedSQL.getParams();
				sql = toJDBC(dialect.countSql(script, sqlMetaData), usedParams);
				Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), id, sql.getValue(),
						sql.getParams(), showSql);
				page.setTotal(total);
				if (total != null && total > 0) {
					page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
					sql = toJDBC(dialect.pageSql(con, script, usedParams, sqlMetaData, pageSize, currentPage),
							usedParams);
					page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), id, sql.getValue(),
							sql.getParams(), showSql));
				} else {
					page.setTotalPage(0L);
				}
			}
		} catch (SQLException e) {
			throw new SQLExecutorException(e);
		} finally {
			Paging.clear();
			JDBCUtils.close(con);
		}
		return page;
	}

	@Override
	public <T> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage,
			int pageSize, Object params) {
		return page(getDefaultDataSource(), type, dsql, cntDsql, currentPage, pageSize, params);
	}

	@Override
	public <T> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Object params) {
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
			Paging.initCountEnv(dialect);// 初始化Paging的计数查询SQL解析环境
			NamedSQL countNamedSQL = parse(cntDsql, params);
			Script<List<Object>> sql;
			if (Paging.isCounted()) {
				sql = toJDBC(countNamedSQL.getScript(), countNamedSQL.getParams());
			} else {
				String script = countNamedSQL.getScript();
				sql = toJDBC(dialect.countSql(script, SQLUtils.getSQLMetaData(script)), countNamedSQL.getParams());
			}
			Long total = JDBCExecuteUtils.execute(con, LongResultSQLExecuter.getInstance(), countNamedSQL.getId(),
					sql.getValue(), sql.getParams(), showSql);
			page.setTotal(total);
			if (total != null && total > 0) {
				page.setTotalPage(total % pageSize == 0 ? total / pageSize : total / pageSize + 1);
				Paging.initPageEnv(dialect, con, page);// 初始化Paging的分页查询SQL解析环境
				NamedSQL namedSQL = parse(dsql, params);// 假设存在#[page(……)]，尝试解析分页查询SQL
				if (Paging.isPaged()) {// DSL已被Paging解析为分页查询SQL
					sql = toJDBC(namedSQL.getScript(), namedSQL.getParams());
				} else {
					String script = namedSQL.getScript();
					sql = toJDBC(dialect.pageSql(con, script, namedSQL.getParams(), SQLUtils.getSQLMetaData(script),
							pageSize, currentPage), namedSQL.getParams());
				}
				page.setRows(JDBCExecuteUtils.execute(con, new SelectSQLExecuter<T>(type), namedSQL.getId(),
						sql.getValue(), sql.getParams(), showSql));
			} else {
				page.setTotalPage(0L);
			}
		} catch (SQLException e) {
			throw new SQLExecutorException(e);
		} finally {
			Paging.clear();
			JDBCUtils.close(con);
		}
		return page;
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
	public boolean execute(String dsql, Object params) {
		return execute(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public boolean execute(DataSource dataSource, String dsql, Object params) {
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
	public int executeUpdate(String dsql, Object params) {
		return executeUpdate(getDefaultDataSource(), parse(dsql, params));
	}

	@Override
	public int executeUpdate(DataSource dataSource, String dsql, Object params) {
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
				throw new SQLExecutorException(e);
			}
			throw new TransactionException(e);
		} finally {
			JDBCUtils.close(con);
			CurrentConnectionHolder.remove();
		}
	}

	private NamedSQL parse(String dsql, Object... params) {
		return getDSQLFactory().parse(dsql, params);
	}

	private NamedSQL parse(String dsql, Object params) {
		return getDSQLFactory().parse(dsql, params);
	}

	private Script<List<Object>> toJDBC(NamedSQL namedSQL) {
		return getDSQLFactory().toJDBC(namedSQL);
	}

	private Script<List<Object>> toJDBC(String namedscript, Map<String, ?> params) {
		return getDSQLFactory().toJDBC(namedscript, params);
	}

	private <T> T execute(DataSource dataSource, Object obj, DMLParser dmlParser, SQLExecuter<T> sqlExecuter) {
		DML dml = dmlParser.parse(obj.getClass());
		return execute(dataSource, null, dml.getSql(), EntityUtils.getParams(obj, dml.getFields()), sqlExecuter);
	}

	private <T> T execute(DataSource dataSource, String id, String sql, List<Object> params,
			SQLExecuter<T> sqlExecuter) {
		Connection con = null;
		T result = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(true);
			con.setReadOnly(sqlExecuter.isReadOnly());
			result = JDBCExecuteUtils.execute(con, sqlExecuter, id, sql, params, isShowSql());
		} catch (SQLException e) {
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.close(con);
		}
		return result;
	}

	private <T> int execute(DataSource dataSource, List<T> rows, DMLParser dmlParser) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JDBCExecuteUtils.executeBatch(con, dmlParser, rows, isShowSql());
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.close(con);
		}
	}

	private <T> void executeBatch(DataSource dataSource, List<T> rows, DMLParser dmlParser,
			int batchSize) {
		DML dml = dmlParser.parse(rows.get(0).getClass());
		JDBCExecuteUtils.executeBatch(dataSource, dml.getSql(), rows, dml.getFields(), batchSize, isShowSql());
	}

	private static <T> int update(DataSource dataSource, boolean showSql, List<T> rows, UpdateSQL updateSQL) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JDBCExecuteUtils.update(con, updateSQL, rows, showSql);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.close(con);
		}
	}

	private <T> void updateBatch(DataSource dataSource, int batchSize, List<T> rows,
			String... hardFields) {
		if (CollectionUtils.isEmpty(rows)) {
			return;
		}
		JDBCExecuteUtils.updateBatch(dataSource, getSQLDialect(dataSource).update(rows.get(0).getClass(), hardFields),
				rows, batchSize, isShowSql());
	}

	private static <T> int save(DataSource dataSource, boolean showSql, List<T> rows, MergeSQL mergeSql) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			int count = JDBCExecuteUtils.save(con, mergeSql, rows, showSql);
			con.commit();
			return count;
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.close(con);
		}
	}

	private <T> T get(DataSource dataSource, NamedSQL namedSQL, Class<T> type) {
		return execute(dataSource, namedSQL, new GetSQLExecuter<T>(type));
	}

	private <T> T execute(DataSource dataSource, NamedSQL namedSQL, SQLExecuter<T> sqlExecuter) {
		Script<List<Object>> sql = toJDBC(namedSQL);
		return execute(dataSource, namedSQL.getId(), sql.getValue(), sql.getParams(), sqlExecuter);
	}

	private <T> List<T> select(DataSource dataSource, NamedSQL namedSQL, Class<T> type) {
		return execute(dataSource, namedSQL, new SelectSQLExecuter<T>(type));
	}

	private boolean execute(DataSource dataSource, NamedSQL namedSQL) {
		return execute(dataSource, namedSQL, ExecuteSQLExecuter.getInstance());
	}

	private int executeUpdate(DataSource dataSource, NamedSQL namedSQL) {
		return execute(dataSource, namedSQL, ExecuteUpdateSQLExecuter.getInstance());
	}

}
