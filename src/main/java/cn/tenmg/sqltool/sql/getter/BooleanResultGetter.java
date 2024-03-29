package cn.tenmg.sqltool.sql.getter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link java.lang.Boolean} 类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class BooleanResultGetter extends AbstractResultGetter<Boolean> {

	@Override
	public Boolean getValue(ResultSet rs, int columnIndex) throws SQLException {
		return toBoolean(rs.getObject(columnIndex));
	}

	@Override
	public Boolean getValue(ResultSet rs, String columnLabel) throws SQLException {
		return toBoolean(rs.getObject(columnLabel));
	}

	private static Boolean toBoolean(Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return Boolean.valueOf(value.toString());
	}

}
