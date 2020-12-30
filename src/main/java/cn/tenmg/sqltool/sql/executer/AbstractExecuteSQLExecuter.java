package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.tenmg.sqltool.sql.SQLExecuter;

/**
 * 抽象执行类SQL执行器
 * 
 * @author 赵伟均
 *
 * @param <T>
 */
public abstract class AbstractExecuteSQLExecuter<T> implements SQLExecuter<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5664359106430145890L;

	@Override
	public ResultSet execute(PreparedStatement ps) throws SQLException {
		return null;
	}

}