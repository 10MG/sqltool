package cn.tenmg.sqltool.sql.engine;

/**
 * Spark SQL 方言的SQL引擎
 * 
 * @author 赵伟均
 *
 */
public class SparkSQLEngine extends BasicSQLEngine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4709716690186443192L;

	private static class InstanceHolder {
		private static final SparkSQLEngine INSTANCE = new SparkSQLEngine();
	}

	public static final SparkSQLEngine getInstance() {
		return InstanceHolder.INSTANCE;
	}

}
