package cn.tenmg.sqltool.sql.getter;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * java.sql.Date类型结果获取器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class SqlDateResultGetter extends AbstractResultGetter<Date> {

	@Override
	public Date getValue(ResultSet rs, int columnIndex) throws SQLException {
		return rs.getDate(columnIndex);
	}

	@Override
	public Date getValue(ResultSet rs, String columnLabel) throws SQLException {
		return rs.getDate(columnLabel);
	}

}
