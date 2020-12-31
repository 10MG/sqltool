package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行SQL的执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class ExecuteSQLExecuter extends AbstractExecuteSQLExecuter<Boolean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8858877740885024842L;

	private static class InstanceHolder {
		private static final ExecuteSQLExecuter INSTANCE = new ExecuteSQLExecuter();
	}

	public static final ExecuteSQLExecuter getInstance() {
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
