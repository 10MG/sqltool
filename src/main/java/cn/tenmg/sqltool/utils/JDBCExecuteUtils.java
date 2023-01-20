package cn.tenmg.sqltool.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.tenmg.dsl.utils.StringUtils;
import cn.tenmg.sql.paging.utils.JDBCUtils;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.SQLExecutorException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;

/**
 * JDBC执行工具类
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.3.0
 */
public abstract class JDBCExecuteUtils {

	private static final Logger log = LoggerFactory.getLogger(JDBCExecuteUtils.class);

	public static final String COMMA_SPACE = ", ", SPACE_AND_SPACE = " AND ", SPACE_EQ_SPACE = " = ";

	private static final String LINE_SPLITOR = System.lineSeparator();

	/**
	 * 执行一个SQL语句
	 * 
	 * @param con
	 *            连接对象
	 * @param sqlExecuter
	 *            SQL执行器
	 * @param id
	 *            DSQL编号
	 * @param sql
	 *            SQL语句
	 * @param params
	 *            参数
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回执行SQL的返回值
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> T execute(Connection con, SQLExecuter<T> sqlExecuter, String id, String sql, List<Object> params,
			boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql);
			JDBCUtils.setParams(ps, params);
			if (showSql && log.isInfoEnabled()) {
				log.info(logPrefix(id, sql).append(sql).append(COMMA_SPACE).append("parameters: ")
						.append(JSONUtils.toJSONString(params)).toString());
			}
			rs = sqlExecuter.execute(ps);
			return sqlExecuter.execute(ps, rs);
		} finally {
			JDBCUtils.close(rs);
			JDBCUtils.close(ps);
		}
	}

	/**
	 * 使用实体对象列表批处理插入、更新或删除数据
	 * 
	 * @param con
	 *            连接对象
	 * @param dmlParser
	 *            数据库操纵对象
	 * @param rows
	 *            实体对象列表
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T extends Serializable> int executeBatch(Connection con, DMLParser dmlParser, List<T> rows,
			boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		try {
			DML dml = dmlParser.parse(rows.get(0).getClass());
			String sql = dml.getSql();
			List<Field> fields = dml.getFields();
			ps = con.prepareStatement(sql);
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
		} finally {
			JDBCUtils.clear(ps);
		}
	}

	/**
	 * 使用实体对象列表分批插入或更新数据
	 * 
	 * @param dataSource
	 *            数据源
	 * @param mergeSql
	 *            合并数据SQL
	 * @param rows
	 *            实体对象列表
	 * @param batchSize
	 *            批容量
	 * @param showSql
	 *            是否打印SQL
	 */
	public static <T> void saveBatch(DataSource dataSource, MergeSQL mergeSql, List<T> rows, int batchSize,
			boolean showSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			ps = con.prepareStatement(sql);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					addBatch(ps, fieldMetas, rows.get(i));
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.clear(ps);
			JDBCUtils.close(con);
		}
	}

	/**
	 * 使用实体对象列表分批执行SQL
	 * 
	 * @param dataSource
	 *            数据源
	 * @param sql
	 *            SQL
	 * @param rows
	 *            实体对象列表
	 * @param fields
	 *            字段列表
	 * @param batchSize
	 *            批容量
	 * @param showSql
	 *            是否打印SQL
	 */
	public static <T extends Serializable> void executeBatch(DataSource dataSource, String sql, List<T> rows,
			List<Field> fields, int batchSize, boolean showSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			ps = con.prepareStatement(sql);
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					addBatch(ps, rows.get(i), fields);
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.clear(ps);
			JDBCUtils.close(con);
		}
	}

	/**
	 * 使用实体对象列表分批执行更新SQL
	 * 
	 * @param dataSource
	 *            数据源
	 * @param updateSql
	 *            更新SQL
	 * @param rows
	 *            实体对象列表
	 * @param batchSize
	 *            批容量
	 * @param showSql
	 *            是否打印SQL
	 */
	public static <T> void updateBatch(DataSource dataSource, UpdateSQL updateSql, List<T> rows, int batchSize,
			boolean showSql) {
		Connection con = null;
		PreparedStatement ps = null;
		try {
			int size = rows.size(), current = 0, times = (int) Math.ceil(size / (double) batchSize);
			con = dataSource.getConnection();
			con.setAutoCommit(false);
			con.setReadOnly(false);
			String sql = updateSql.getScript();
			List<Field> fields = updateSql.getFields();
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			ps = con.prepareStatement(sql);
			while (current < times) {
				int end = (current + 1) * batchSize, last = end < size ? end : size;
				for (int i = current * batchSize; i < last; i++) {
					addBatch(ps, rows.get(i), fields);
				}
				ps.executeBatch();
				con.commit();
				ps.clearBatch();
				current++;
			}
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			throw new SQLExecutorException(e);
		} finally {
			JDBCUtils.clear(ps);
			JDBCUtils.close(con);
		}
	}

	/**
	 * 使用实体对象列表软更新数据
	 * 
	 * @param con
	 *            连接对象
	 * @param updateSQL
	 *            更新数据操作对象
	 * @param rows
	 *            实体对象列表
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int update(Connection con, UpdateSQL updateSQL, List<T> rows, boolean showSql)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			String sql = updateSQL.getScript();
			List<Field> fields = updateSQL.getFields();
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
		} finally {
			JDBCUtils.clear(ps);
		}
	}

	/**
	 * 使用实体对象列表硬更新数据
	 * 
	 * @param con
	 *            连接对象
	 * @param rows
	 *            实体对象列表
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T extends Serializable> int hardUpdate(Connection con, List<T> rows, boolean showSql)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			DML dml = UpdateDMLParser.getInstance().parse(rows.get(0).getClass());
			String sql = dml.getSql();
			List<Field> fields = dml.getFields();
			ps = con.prepareStatement(sql);
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
		} finally {
			JDBCUtils.clear(ps);
		}
	}

	/**
	 * 使用实体对象列表软保存数据
	 * 
	 * @param con
	 *            连接对象
	 * @param mergeSql
	 *            合并数据操作对象
	 * @param rows
	 *            实体对象列表
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int save(Connection con, MergeSQL mergeSql, List<T> rows, boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		try {
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			return getCount(ps.executeBatch());
		} finally {
			JDBCUtils.clear(ps);
		}
	}

	/**
	 * 使用实体对象列表硬保存数据
	 * 
	 * @param con
	 *            连接对象
	 * @param dialect
	 *            SQL方言
	 * @param rows
	 *            实体对象列表
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int hardSave(Connection con, SQLDialect dialect, List<T> rows, boolean showSql)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			MergeSQL mergeSql = dialect.hardSave(rows.get(0).getClass());
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql && log.isInfoEnabled()) {
				log(sql);
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			return getCount(ps.executeBatch());
		} finally {
			JDBCUtils.clear(ps);
		}
	}

	/**
	 * 添加一个批量
	 * 
	 * @param ps
	 *            准备声明对象
	 * @param fieldMetas
	 *            属性元数据列表
	 * @param obj
	 *            实体对象
	 * @throws SQLException
	 *             SQL异常
	 */
	private static final <T> void addBatch(PreparedStatement ps, List<FieldMeta> fieldMetas, T obj)
			throws SQLException {
		for (int i = 0, size = fieldMetas.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fieldMetas.get(i).getField().get(obj));
			} catch (Exception e) {
				throw new DataAccessException(e);
			}
		}
		ps.addBatch();
	}

	/**
	 * 添加一个批量
	 * 
	 * @param ps
	 *            准备声明对象
	 * @param obj
	 *            实体对象
	 * @param fields
	 *            属性列表
	 * @throws SQLException
	 *             SQL异常
	 */
	private static final <T> void addBatch(PreparedStatement ps, T obj, List<Field> fields) throws SQLException {
		for (int i = 0, size = fields.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fields.get(i).get(obj));
			} catch (Exception e) {
				throw new DataAccessException(e);
			}
		}
		ps.addBatch();
	}

	/**
	 * 根据批量提交返回结果集汇总影响行数
	 * 
	 * @param counts
	 *            批量提交返回结果
	 * @return 返回汇总的影响行数
	 */
	private static int getCount(int[] counts) {
		int count = 0;
		if (counts != null) {
			for (int i = 0; i < counts.length; i++) {
				count += counts[i];
			}
		}
		return count;
	}

	private static void log(String sql) {
		if (StringUtils.isNotBlank(sql)) {
			char c = sql.charAt(0);
			if (c == '\r' || c == '\n') {
				log.info("Execute SQL:".concat(sql));
				return;
			}
		}
		log.info("Execute SQL:\n".concat(sql));
	}

	private static StringBuilder logPrefix(String id, String sql) {
		StringBuilder logPrefix = new StringBuilder();
		if (StringUtils.isNotBlank(sql)) {
			char c = sql.charAt(0);
			if (c == '\r' || c == '\n') {
				if (id == null) {
					return logPrefix.append("Execute SQL:");
				} else {
					return logPrefix.append("Execute SQL(").append(id).append("):");
				}
			}
		}
		if (id == null) {
			return logPrefix.append("Execute SQL:").append(LINE_SPLITOR);
		} else {
			return logPrefix.append("Execute SQL(").append(id).append("):").append(LINE_SPLITOR);
		}
	}
}
