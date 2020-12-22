package cn.tenmg.sqltool.sql;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * SQL 执行器
 * 
 * @author 赵伟均
 *
 * @param <T>
 *            实体类
 */
public interface SqlExecuter<T> extends Serializable {

	ResultSet execute(PreparedStatement ps) throws SQLException;

	T execute(PreparedStatement ps, ResultSet rs) throws SQLException;
}
