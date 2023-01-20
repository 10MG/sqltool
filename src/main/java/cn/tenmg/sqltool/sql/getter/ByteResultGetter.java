package cn.tenmg.sqltool.sql.getter;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * java.lang.Byte类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class ByteResultGetter extends AbstractResultGetter<Byte> {

	@Override
	public Byte getValue(ResultSet rs, int columnIndex) throws SQLException {
		return toByte(rs.getObject(columnIndex));
	}

	@Override
	public Byte getValue(ResultSet rs, String columnLabel) throws SQLException {
		return toByte(rs.getObject(columnLabel));
	}

	private static Byte toByte(Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof Byte) {
			return (Byte) value;
		}
		return Byte.valueOf(value.toString());
	}

}
