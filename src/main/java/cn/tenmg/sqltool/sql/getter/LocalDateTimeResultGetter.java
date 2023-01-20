package cn.tenmg.sqltool.sql.getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * java.time.LocalDateTime类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class LocalDateTimeResultGetter extends AbstractResultGetter<LocalDateTime> {

	@Override
	public LocalDateTime getValue(ResultSet rs, int columnIndex) throws SQLException {
		return (LocalDateTime) rs.getObject(columnIndex);
	}

	@Override
	public LocalDateTime getValue(ResultSet rs, String columnLabel) throws SQLException {
		return (LocalDateTime) rs.getObject(columnLabel);
	}

}
