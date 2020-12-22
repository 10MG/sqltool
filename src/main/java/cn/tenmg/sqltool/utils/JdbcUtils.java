package cn.tenmg.sqltool.utils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.sql.meta.FieldMeta;

/**
 * JDBC工具类
 * 
 * @author 赵伟均
 *
 */
public abstract class JdbcUtils {

	private static final Logger log = LoggerFactory.getLogger(JdbcUtils.class);

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
				if (log.isErrorEnabled()) {
					log.error("Could not close JDBC Connection", ex);
				}
				ex.printStackTrace();
			} catch (Throwable ex) {
				if (log.isErrorEnabled()) {
					log.error("Unexpected exception on closing JDBC Connection", ex);
				}
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
				if (log.isErrorEnabled()) {
					log.error("Could not close JDBC Statement", ex);
				}
				ex.printStackTrace();
			} catch (Throwable ex) {
				if (log.isErrorEnabled()) {
					log.error("Unexpected exception on closing JDBC Statement", ex);
				}
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
				if (log.isErrorEnabled()) {
					log.error("Could not close JDBC ResultSet", ex);
				}
			} catch (Throwable ex) {
				if (log.isErrorEnabled()) {
					log.error("Unexpected exception on closing JDBC ResultSet", ex);
				}
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
}
