package cn.tenmg.sqltool.utils;

import cn.tenmg.sqltool.exception.NosuitableSqlEngineException;
import cn.tenmg.sqltool.sql.SqlEngine;
import cn.tenmg.sqltool.sql.engine.MySQLSqlEngine;
import cn.tenmg.sqltool.sql.engine.OracleSqlEngine;
import cn.tenmg.sqltool.sql.engine.SparkSQLSqlEngine;

/**
 * SQL引擎工具
 * 
 * @author 赵伟均
 *
 */
public abstract class SqlEngineUtils {
	/**
	 * 根据连接地址获取SQL引擎
	 * 
	 * @param url
	 *            连接地址
	 * @return url为null时，返回SparkSQLSqlEngine；如果无法找到合适的引擎，将抛出NosuitableSqlEngineException异常；否则，返回所对应的SQL引擎
	 */
	public static final SqlEngine getSqlEngine(String url) {
		if (url == null) {
			return SparkSQLSqlEngine.getInstance();
		} else if (url.contains("mysql")) {
			return MySQLSqlEngine.getInstance();
		} else if (url.contains("oracle")) {
			return OracleSqlEngine.getInstance();
		}
		throw new NosuitableSqlEngineException("There is no suitable SQL engine here for url: " + url);
	}
}
