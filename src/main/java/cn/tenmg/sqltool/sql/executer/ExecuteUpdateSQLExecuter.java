package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行更新的SQL执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class ExecuteUpdateSQLExecuter extends AbstractExecuteSQLExecuter<Integer> {

	private static class InstanceHolder {
		private static final ExecuteUpdateSQLExecuter INSTANCE = new ExecuteUpdateSQLExecuter();
	}

	public static final ExecuteUpdateSQLExecuter getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	public Integer execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		return ps.executeUpdate();
	}

}
