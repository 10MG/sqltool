package cn.tenmg.sqltool.sql.executer;

/**
 * 返回<code>java.lang.Long</code>查询结果类型的SQL执行器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class LongResultSQLExecuter extends GetSQLExecuter<Long> {

	private static class InstanceHolder {
		private static final LongResultSQLExecuter INSTANCE = new LongResultSQLExecuter();
	}

	public static final LongResultSQLExecuter getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private LongResultSQLExecuter() {
		super();
	}
}
