package cn.tenmg.sqltool.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL 执行器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @param <T> 实体类
 * @since 1.1.1
 */
public interface SQLExecuter<T> {

	@Deprecated
	boolean isReadOnly();

	ResultSet execute(PreparedStatement ps) throws SQLException;

	T execute(PreparedStatement ps, ResultSet rs) throws SQLException;
}
