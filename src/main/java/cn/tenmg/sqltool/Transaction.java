package cn.tenmg.sqltool;

import java.io.Serializable;

import cn.tenmg.sqltool.SqltoolContext.SqltoolExecutor;

/**
 * 事务
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface Transaction extends Serializable {

	/**
	 * 执行事务方法。该方法内的操作在一个数据库事务内。方法结束后通过executor对数据库的操作事务才会最终提交，一旦发生异常，事务将回滚。
	 * 
	 * @param executor
	 *            Sqltool执行对象
	 * @throws Exception
	 */
	void execute(SqltoolExecutor executor) throws Exception;
}
