package cn.tenmg.sqltool.sql.executer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.utils.FieldUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 查询单条记录的数据的SQL执行器
 * 
 * @author 赵伟均
 *
 * @param <T>
 *            实体类
 */
public class GetSQLExecuter<T> implements SQLExecuter<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5489699234751455394L;

	protected Class<T> type;

	@SuppressWarnings("unchecked")
	public GetSQLExecuter() {
		type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public GetSQLExecuter(Class<T> type) {
		this.type = type;
	}

	@Override
	public ResultSet execute(PreparedStatement ps) throws SQLException {
		return ps.executeQuery();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		T row = null;
		if (rs.next()) {
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			if (BigDecimal.class.isAssignableFrom(type)) {
				row = (T) rs.getBigDecimal(1);
			} else if (Number.class.isAssignableFrom(type)) {
				Object obj = rs.getObject(1);
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
				row = (T) obj;
			} else if (String.class.isAssignableFrom(type)) {
				row = (T) rs.getString(1);
			} else if (BigDecimal.class.isAssignableFrom(type)) {
				row = (T) rs.getBigDecimal(1);
			} else if (Date.class.isAssignableFrom(type)) {
				row = (T) rs.getObject(1);
			} else {
				Map<String, Integer> feildNames = new HashMap<String, Integer>();
				for (int i = 1; i <= columnCount; i++) {
					String feildName = StringUtils.toCamelCase(rsmd.getColumnLabel(i), "_", false);
					feildNames.put(feildName, i);
				}
				Map<Integer, Field> fieldMap = new HashMap<Integer, Field>();
				Class<?> current = type;
				while (!Object.class.equals(current)) {
					FieldUtils.parseFields(feildNames, fieldMap, current.getDeclaredFields());
					current = current.getSuperclass();
				}
				try {
					row = type.newInstance();
					for (int i = 1; i <= columnCount; i++) {
						Field field = fieldMap.get(i);
						if (field != null) {
							field.set(row, JdbcUtils.getValue(rs, i, field.getType()));
						}
					}
				} catch (InstantiationException | IllegalAccessException e) {
					throw new DataAccessException(e);
				}
			}
		}
		return row;
	}

}
