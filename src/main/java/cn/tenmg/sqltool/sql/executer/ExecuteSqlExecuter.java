package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行SQL的执行器
 * 
 * @author 赵伟均
 *
 */
public class ExecuteSqlExecuter extends AbstractExecuteSqlExecuter<Boolean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8858877740885024842L;

	private static class InstanceHolder {
		private static final ExecuteSqlExecuter INSTANCE = new ExecuteSqlExecuter();
	}

	public static final ExecuteSqlExecuter getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	public ResultSet execute(PreparedStatement ps) throws SQLException {
		return null;
	}

	@Override
	public Boolean execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		return ps.execute();
	}

}
