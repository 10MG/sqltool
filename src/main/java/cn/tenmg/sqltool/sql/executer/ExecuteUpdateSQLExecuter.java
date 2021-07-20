package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 执行更新的SQL执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 * @since 1.1.1
 */
public class ExecuteUpdateSQLExecuter extends AbstractExecuteSQLExecuter<Integer> {

	private static final ExecuteUpdateSQLExecuter INSTANCE = new ExecuteUpdateSQLExecuter();

	private ExecuteUpdateSQLExecuter() {
		super();
	}

	public static final ExecuteUpdateSQLExecuter getInstance() {
		return INSTANCE;
	}

	@Override
	public Integer execute(PreparedStatement ps, ResultSet rs) throws SQLException {
		return ps.executeUpdate();
	}

}
