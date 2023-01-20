package cn.tenmg.sqltool.sql.getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * java.sql.Timestamp类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class TimestampResultGetter extends AbstractResultGetter<Timestamp> {

	@Override
	public Timestamp getValue(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getTimestamp(columnIndex);
	}

	@Override
	public Timestamp getValue(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getTimestamp(columnLabel);
	}

}
