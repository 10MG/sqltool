package cn.tenmg.sqltool.sql.engine;

/**
 * Spark SQL 方言的SQL引擎
 * 
 * @author 赵伟均
 *
 */
public class SparkSQLSqlEngine extends BasicSqlEngine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4709716690186443192L;

	private static class InstanceHolder {
		private static final SparkSQLSqlEngine INSTANCE = new SparkSQLSqlEngine();
	}

	public static final SparkSQLSqlEngine getInstance() {
		return InstanceHolder.INSTANCE;
	}

}
