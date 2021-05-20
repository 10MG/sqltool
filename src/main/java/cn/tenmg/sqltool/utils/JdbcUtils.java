package cn.tenmg.sqltool.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.DMLParser;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.SQLMetaData;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.UpdateDMLParser;

/**
 * JDBC工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class JdbcUtils {

	private static final Logger log = LogManager.getLogger(JdbcUtils.class);

	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n"), COMMA_SPACE = ", ",
			SPACE_AND_SPACE = " AND ", SPACE_EQ_SPACE = " = ";

	public static final char PARAM_MARK = '?', SINGLE_QUOTATION_MARK = '\'';

	private JdbcUtils() {
	}

	/**
	 * 关闭连接
	 * 
	 * @param conn
	 */
	public static void close(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ex) {
				log.error("Could not close JDBC Connection", ex);
				ex.printStackTrace();
			} catch (Throwable ex) {
				log.error("Unexpected exception on closing JDBC Connection", ex);
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 关闭声明
	 * 
	 * @param stm
	 *            声明
	 */
	public static void close(Statement stm) {
		if (stm != null) {
			try {
				stm.close();
			} catch (SQLException ex) {
				log.error("Could not close JDBC Statement", ex);
				ex.printStackTrace();
			} catch (Throwable ex) {
				log.error("Unexpected exception on closing JDBC Statement", ex);
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 关闭结果集
	 * 
	 * @param rs
	 *            结果集
	 */
	public static void close(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException ex) {
				log.error("Could not close JDBC ResultSet", ex);
			} catch (Throwable ex) {
				log.error("Unexpected exception on closing JDBC ResultSet", ex);
			}
		}
	}

	/**
	 * 从实体对象中获取属性参数集
	 * 
	 * @param obj
	 *            实体对象
	 * @param fields
	 *            参数属性集
	 * @return 返回参数集
	 */
	public static <T> List<Object> getParams(T obj, List<Field> fields) {
		List<Object> params = new ArrayList<Object>();
		for (int i = 0, size = fields.size(); i < size; i++) {
			try {
				params.add(fields.get(i).get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new DataAccessException(e);
			}
		}
		return params;
	}

	/**
	 * 设置参数
	 * 
	 * @param ps
	 *            SQL声明对象
	 * @param params
	 *            查询参数
	 * @throws SQLException
	 */
	public static void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
		if (!CollectionUtils.isEmpty(params)) {
			for (int i = 0, size = params.size(); i < size; i++) {
				ps.setObject(i + 1, params.get(i));
			}
		}
	}

	/**
	 * 设置参数
	 * 
	 * @param ps
	 *            SQL声明对象
	 * @param obj
	 *            实体对象
	 * @param fields
	 *            参数属性集
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> void setParams(PreparedStatement ps, T obj, List<Field> fields) throws SQLException {
		for (int i = 0, size = fields.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fields.get(i).get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new DataAccessException(e);
			}
		}
	}

	/**
	 * 设置参数
	 * 
	 * @param ps
	 *            SQL声明对象
	 * @param fieldMetas
	 *            参数属性集
	 * @param obj
	 *            实体对象
	 * @throws SQLException
	 *             SQL异常
	 */
	public static <T> void setParams(PreparedStatement ps, List<FieldMeta> fieldMetas, T obj) throws SQLException {
		for (int i = 0, size = fieldMetas.size(); i < size; i++) {
			try {
				ps.setObject(i + 1, fieldMetas.get(i).getField().get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new DataAccessException(e);
			}
		}
	}

	/**
	 * 获取结果当前行集指定列的值
	 * 
	 * @param rs
	 *            结果集
	 * @param columnIndex
	 *            指定列索引
	 * @param type
	 *            值的类型
	 * @return 返回当前行集指定列的值
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(ResultSet rs, int columnIndex, Class<T> type) throws SQLException {
		if (BigDecimal.class.isAssignableFrom(type)) {
			return (T) rs.getBigDecimal(columnIndex);
		} else if (Number.class.isAssignableFrom(type)) {
			Object obj = rs.getObject(columnIndex);
			if (obj == null) {
				return null;
			}
			if (obj instanceof Number) {
				if (Double.class.isAssignableFrom(type)) {
					obj = ((Number) obj).doubleValue();
				} else if (Float.class.isAssignableFrom(type)) {
					obj = ((Number) obj).floatValue();
				} else if (Integer.class.isAssignableFrom(type)) {
					obj = ((Number) obj).intValue();
				} else if (Long.class.isAssignableFrom(type)) {
					obj = ((Number) obj).longValue();
				} else if (Short.class.isAssignableFrom(type)) {
					obj = ((Number) obj).shortValue();
				} else if (Byte.class.isAssignableFrom(type)) {
					obj = ((Number) obj).byteValue();
				}
			}
			return (T) obj;
		} else if (String.class.isAssignableFrom(type)) {
			return (T) rs.getString(columnIndex);
		}
		return (T) rs.getObject(columnIndex);
	}

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
			setParams(ps, params);
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
			close(rs);
			close(ps);
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
			close(ps);
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
			close(ps);
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
			close(ps);
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
			close(ps);
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
				close(ps);
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
		setParams(ps, fieldMetas, obj);
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
		setParams(ps, obj, fields);
		ps.addBatch();
	}

	private static final String SELECT_ALL = "SELECT * FROM (\n", ALIAS = "\n) SQLTOOL", WHERE_IMPOSSIBLE = "\nWHERE 1=0";

	/**
	 * 获取SQL字段名列表
	 * 
	 * @param con
	 *            已打开的数据库连接
	 * @param sql
	 *            SQL
	 * @param params
	 *            查询参数集
	 * @param sqlMetaData
	 *            SQL相关数据对象
	 * @return 返回SQL字段名列表
	 * @throws SQLException
	 *             SQL异常
	 */
	public static final String[] getColumnLabels(Connection con, String sql, List<Object> params,
			SQLMetaData sqlMetaData) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String script, columnLabels[] = null;
		try {
			int length = sqlMetaData.getLength(), embedStartIndex = sqlMetaData.getEmbedStartIndex(),
					embedEndIndex = sqlMetaData.getEmbedEndIndex();
			if (embedStartIndex > 0) {
				if (embedEndIndex < length) {
					script = sql.substring(0, embedStartIndex).concat(SELECT_ALL)
							.concat(sql.substring(embedStartIndex, embedEndIndex)).concat(ALIAS)
							.concat(WHERE_IMPOSSIBLE).concat(sql.substring(embedEndIndex));
				} else {
					script = sql.substring(0, embedStartIndex).concat(SELECT_ALL).concat(sql.substring(embedStartIndex))
							.concat(ALIAS).concat(WHERE_IMPOSSIBLE);
				}
			} else {
				if (embedEndIndex < length) {
					script = SELECT_ALL.concat(sql.substring(0, embedEndIndex)).concat(ALIAS).concat(WHERE_IMPOSSIBLE)
							.concat(sql.substring(embedEndIndex));
				} else {
					script = SELECT_ALL.concat(sql).concat(ALIAS).concat(WHERE_IMPOSSIBLE);
				}
			}
			ps = con.prepareStatement(script);
			setParams(ps, params);
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			columnLabels = new String[columnCount];
			for (int i = 1; i <= columnCount; i++) {
				columnLabels[i - 1] = rsmd.getColumnLabel(i);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			JdbcUtils.close(rs);
			JdbcUtils.close(ps);
		}
		return columnLabels;
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
