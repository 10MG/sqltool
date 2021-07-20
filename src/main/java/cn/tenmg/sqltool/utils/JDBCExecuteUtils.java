package cn.tenmg.sqltool.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.tenmg.sql.paging.utils.JDBCUtils;
import cn.tenmg.sqltool.exception.DataAccessException;
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
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.3.0
 */
public abstract class JDBCExecuteUtils {

	private static final Logger log = LogManager.getLogger(JDBCExecuteUtils.class);

	public static final String COMMA_SPACE = ", ", SPACE_AND_SPACE = " AND ", SPACE_EQ_SPACE = " = ";

	/**
	 * 执行一个SQL语句
	 * 
	 * @param con
	 *            连接对象
	 * @param sql
	 *            SQL语句
	 * @param params
	 *            参数
	 * @param sqlExecuter
	 *            SQL执行器
	 * @param showSql
	 *            是否打印SQL
	 * @return 返回执行SQL的返回值
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> T execute(Connection con, String id, String sql, List<Object> params, SQLExecuter<T> sqlExecuter,
			boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql);
			JDBCUtils.setParams(ps, params);
			if (showSql && log.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Execute SQL: ").append(sql).append(COMMA_SPACE).append("parameters: ")
						.append(JSONUtils.toJSONString(params));
				if (id != null) {
					sb.append(COMMA_SPACE).append("id: ").append(id);
				}
				log.info(sb.toString());
			}
			rs = sqlExecuter.execute(ps);
			return sqlExecuter.execute(ps, rs);
		} catch (SQLException e) {
			throw e;
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
	 * @param showSql
	 *            是否打印SQL
	 * @param rows
	 *            实体对象列表
	 * @param dmlParser
	 *            数据库操纵对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T extends Serializable> int executeBatch(Connection con, boolean showSql, List<T> rows,
			DMLParser dmlParser) throws SQLException {
		PreparedStatement ps = null;
		try {
			DML dml = dmlParser.parse(rows.get(0).getClass());
			String sql = dml.getSql();
			List<Field> fields = dml.getFields();
			ps = con.prepareStatement(sql);
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
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
			JDBCUtils.close(ps);
		}
	}

	/**
	 * 使用实体对象列表软更新数据
	 * 
	 * @param con
	 *            连接对象
	 * @param showSql
	 *            是否打印SQL
	 * @param rows
	 *            实体对象列表
	 * @param updateSQL
	 *            更新数据操作对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int update(Connection con, boolean showSql, List<T> rows, UpdateSQL updateSQL)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			String sql = updateSQL.getScript();
			List<Field> fields = updateSQL.getFields();
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw e;
		} catch (Exception e) {
			throw new DataAccessException(e);
		} finally {
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			JDBCUtils.close(ps);
		}
	}

	/**
	 * 使用实体对象列表硬更新数据
	 * 
	 * @param con
	 *            连接对象
	 * @param showSql
	 *            是否打印SQL
	 * @param rows
	 *            实体对象列表
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T extends Serializable> int hardUpdate(Connection con, boolean showSql, List<T> rows)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			DML dml = UpdateDMLParser.getInstance().parse(rows.get(0).getClass());
			String sql = dml.getSql();
			List<Field> fields = dml.getFields();
			ps = con.prepareStatement(sql);
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			return getCount(ps.executeBatch());
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
			JDBCUtils.close(ps);
		}
	}

	/**
	 * 使用实体对象列表软保存数据
	 * 
	 * @param con
	 *            连接对象
	 * @param showSql
	 *            是否打印SQL
	 * @param rows
	 *            实体对象列表
	 * @param mergeSql
	 *            合并数据操作对象
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int save(Connection con, boolean showSql, List<T> rows, MergeSQL mergeSql) throws SQLException {
		PreparedStatement ps = null;
		try {
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			return getCount(ps.executeBatch());
		} catch (SQLException e) {
			try {
				con.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			throw e;
		} catch (Exception e) {
			throw new DataAccessException(e);
		} finally {
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			JDBCUtils.close(ps);
		}
	}

	/**
	 * 使用实体对象列表硬保存数据
	 * 
	 * @param con
	 *            连接对象
	 * @param showSql
	 *            是否打印SQL
	 * @param rows
	 *            实体对象列表
	 * @return 返回受影响行数
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> int hardSave(Connection con, SQLDialect dialect, boolean showSql, List<T> rows)
			throws SQLException {
		PreparedStatement ps = null;
		try {
			MergeSQL mergeSql = dialect.hardSave(rows.get(0).getClass());
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql && log.isInfoEnabled()) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			return getCount(ps.executeBatch());
		} catch (SQLException e) {
			throw e;
		} finally {
			if (ps != null) {
				try {
					ps.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				JDBCUtils.close(ps);
			}
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
	public static final <T> void addBatch(PreparedStatement ps, List<FieldMeta> fieldMetas, T obj) throws SQLException {
		for (int i = 0, size = fieldMetas.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fieldMetas.get(i).getField().get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
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
	public static final <T> void addBatch(PreparedStatement ps, T obj, List<Field> fields) throws SQLException {
		for (int i = 0, size = fields.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fields.get(i).get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
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
}
