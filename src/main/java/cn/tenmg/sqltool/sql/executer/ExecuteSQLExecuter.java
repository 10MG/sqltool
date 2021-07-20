package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行SQL的执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.1.1
 */
public class ExecuteSQLExecuter extends AbstractExecuteSQLExecuter<Boolean> {

	private static final ExecuteSQLExecuter INSTANCE = new ExecuteSQLExecuter();

	private ExecuteSQLExecuter() {
		super();
	}

	public static final ExecuteSQLExecuter getInstance() {
		return INSTANCE;
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
