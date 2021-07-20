package cn.tenmg.sqltool;

import java.io.Serializable;

import cn.tenmg.sqltool.transaction.TransactionExecutor;

/**
 * 事务
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
public interface Transaction extends Serializable {

	/**
	 * 执行事务方法。该方法内的操作在一个数据库事务内。方法结束后通过executor对数据库的操作事务才会最终提交，一旦发生异常，事务将回滚。
	 * 
	 * @param executor
	 *            事务执行对象
	 * @throws Exception
	 */
	void execute(TransactionExecutor executor) throws Exception;
}
