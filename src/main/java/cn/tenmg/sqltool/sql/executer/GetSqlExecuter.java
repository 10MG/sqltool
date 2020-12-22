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
import cn.tenmg.sqltool.sql.SqlExecuter;
import cn.tenmg.sqltool.sql.utils.FieldUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 查询单条记录的数据的SQL执行器
 * 
 * @author 赵伟均
 *
 * @param <T>
 *            实体类
 */
public class GetSqlExecuter<T> implements SqlExecuter<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5489699234751455394L;

	protected Class<T> type;

	@SuppressWarnings("unchecked")
	public GetSqlExecuter() {
		type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public GetSqlExecuter(Class<T> type) {
		this.type = type;
	}

	@Override
	public ResultSet execute(PreparedStatement ps) throws SQLException {
		return ps.executeQuery();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		T row = null;
		if (String.class.isAssignableFrom(type) || Number.class.isAssignableFrom(type)
				|| Date.class.isAssignableFrom(type) || BigDecimal.class.isAssignableFrom(type)) {
			if (rs.next()) {
				row = (T) rs.getObject(1);
			}
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
			if (rs.next()) {
				try {
					row = type.newInstance();
					for (int i = 1; i <= columnCount; i++) {
						Field field = fieldMap.get(i);
						if (field != null) {
							field.set(row, rs.getObject(i));
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
