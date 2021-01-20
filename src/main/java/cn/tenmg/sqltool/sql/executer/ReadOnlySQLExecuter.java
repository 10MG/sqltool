package cn.tenmg.sqltool.sql.executer;

import cn.tenmg.sqltool.sql.SQLExecuter;

/**
 * 只读SQL执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 * @param <T>
 *            返回结果类型
 */
public abstract class ReadOnlySQLExecuter<T> implements SQLExecuter<T> {

	@Override
	public boolean isReadOnly() {
		return true;
	}

}