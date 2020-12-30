package cn.tenmg.sqltool.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.parser.InsertDMLParser;

/**
 * JDBC工具类
 * 
 * @author 赵伟均
 *
 */
public abstract class JdbcUtils {

	private static final Logger log = Logger.getLogger(JdbcUtils.class);

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

	public static <T> T execute(Connection con, String sql, List<Object> params, SQLExecuter<T> sqlExecuter,
			boolean showSql) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(sql);
			setParams(ps, params);
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
			close(rs);
			close(ps);
		}
	}

	public static <T extends Serializable> int insert(Connection con, boolean showSql, List<T> rows)
			throws SQLException {
		PreparedStatement ps = null;
		int counts[];
		try {
			DML dml = InsertDMLParser.getInstance().parse(rows.get(0).getClass());
			String sql = dml.getSql();
			List<Field> fields = dml.getFields();
			ps = con.prepareStatement(sql);
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, rows.get(i), fields);
			}
			counts = ps.executeBatch();
			con.commit();
			return getCount(counts);
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

	public static <T> int save(Connection con, boolean showSql, List<T> rows, MergeSQL mergeSql) throws SQLException {
		PreparedStatement ps = null;
		int counts[];
		try {
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			counts = ps.executeBatch();
			con.commit();
			return getCount(counts);
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

	public static <T> int hardSave(Connection con, SQLDialect dialect, boolean showSql, List<T> rows)
			throws SQLException {
		PreparedStatement ps = null;
		int counts[];
		try {
			MergeSQL mergeSql = dialect.hardSave(rows.get(0).getClass());
			String sql = mergeSql.getScript();
			List<FieldMeta> fieldMetas = mergeSql.getFieldMetas();
			if (showSql) {
				log.info("Execute SQL: ".concat(sql));
			}
			ps = con.prepareStatement(sql);
			for (int i = 0, size = rows.size(); i < size; i++) {
				addBatch(ps, fieldMetas, rows.get(i));
			}
			counts = ps.executeBatch();
			con.commit();
			return getCount(counts);
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

	public static final <T> void addBatch(PreparedStatement ps, List<FieldMeta> fieldMetas, T row) throws SQLException {
		setParams(ps, fieldMetas, row);
		ps.addBatch();
	}

	public static final <T> void addBatch(PreparedStatement ps, T row, List<Field> fields) throws SQLException {
		setParams(ps, row, fields);
		ps.addBatch();
	}

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
