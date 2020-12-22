package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行更新的SQL执行器
 * 
 * @author 赵伟均
 *
 */
public class ExecuteUpdateSqlExecuter extends AbstractExecuteSqlExecuter<Integer> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5413892345208619397L;

	private static class InstanceHolder {
		private static final ExecuteUpdateSqlExecuter INSTANCE = new ExecuteUpdateSqlExecuter();
	}

	public static final ExecuteUpdateSqlExecuter getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	public Integer execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		return ps.executeUpdate();
	}

}
