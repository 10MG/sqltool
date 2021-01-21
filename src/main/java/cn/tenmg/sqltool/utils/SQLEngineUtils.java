package cn.tenmg.sqltool.utils;

import cn.tenmg.sqltool.exception.NoSuitableSqlEngineException;
import cn.tenmg.sqltool.sql.SQLEngine;
import cn.tenmg.sqltool.sql.engine.MySQLEngine;
import cn.tenmg.sqltool.sql.engine.OracleEngine;
import cn.tenmg.sqltool.sql.engine.SparkSQLEngine;

/**
 * SQL引擎工具
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class SQLEngineUtils {
	/**
	 * 根据连接地址获取SQL引擎
	 * 
	 * @param url
	 *            连接地址
	 * @return url为null时，返回SparkSQLSqlEngine；如果无法找到合适的引擎，将抛出NosuitableSqlEngineException异常；否则，返回所对应的SQL引擎
	 */
	public static final SQLEngine getSqlEngine(String url) {
		if (url == null) {
			return SparkSQLEngine.getInstance();
		} else if (url.contains("mysql")) {
			return MySQLEngine.getInstance();
		} else if (url.contains("oracle")) {
			return OracleEngine.getInstance();
		}
		throw new NoSuitableSqlEngineException("There is no suitable SQL engine here for url: " + url);
	}
}
