package cn.tenmg.sqltool.sql.engine;

/**
 * MySQL方言的SQL引擎
 * 
 * @author 赵伟均
 *
 */
public class MySQLEngine extends BasicSQLEngine {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3906596407170164697L;

	private static class InstanceHolder {
		private static final MySQLEngine INSTANCE = new MySQLEngine();
	}

	public static final MySQLEngine getInstance() {
		return InstanceHolder.INSTANCE;
	}

}
