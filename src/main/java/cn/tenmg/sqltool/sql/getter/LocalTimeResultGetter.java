package cn.tenmg.sqltool.sql.getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

/**
 * java.time.LocalTime类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class LocalTimeResultGetter extends AbstractResultGetter<LocalTime> {

	@Override
	public LocalTime getValue(ResultSet rs, int columnIndex) throws SQLException {
		return (LocalTime) rs.getObject(columnIndex);
	}

	@Override
	public LocalTime getValue(ResultSet rs, String columnLabel) throws SQLException {
		return (LocalTime) rs.getObject(columnLabel);
	}

}
