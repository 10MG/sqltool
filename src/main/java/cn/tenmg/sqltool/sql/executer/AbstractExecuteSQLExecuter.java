package cn.tenmg.sqltool.sql.executer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cn.tenmg.sqltool.sql.SQLExecuter;

/**
 * 抽象执行类SQL执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 * @param <T>
 *            返回结果类型
 */
public abstract class AbstractExecuteSQLExecuter<T> implements SQLExecuter<T> {

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public ResultSet execute(PreparedStatement ps) throws SQLException {
		return null;
	}

}
