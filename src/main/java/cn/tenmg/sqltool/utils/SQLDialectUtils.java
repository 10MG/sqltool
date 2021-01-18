package cn.tenmg.sqltool.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.tenmg.sqltool.exception.NosuitableSQLDialectExeption;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.dialect.MySQLDialect;
import cn.tenmg.sqltool.sql.dialect.OracleDialect;
import cn.tenmg.sqltool.sql.dialect.PostgreSQLDialect;

/**
 * 方言工具类
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class SQLDialectUtils {

	private static volatile Map<String, SQLDialect> DIALECTS = new HashMap<String, SQLDialect>();

	protected static synchronized void cacheSQLDialect(String url, SQLDialect dialect) {
		DIALECTS.put(url, dialect);
	}

	public static SQLDialect getSQLDialect(Map<String, String> options) {
		return getSQLDialect(options.get("url"));
	}

	public static SQLDialect getSQLDialect(Properties properties) {
		return getSQLDialect(properties.getProperty("url"));
	}

	private static SQLDialect getSQLDialect(String url) {
		SQLDialect dialect = null;
		if (DIALECTS.containsKey(url)) {
			dialect = DIALECTS.get(url);
		} else if (url != null) {
			if (url.contains("mysql")) {
				dialect = MySQLDialect.getInstance();
			} else if (url.contains("oracle")) {
				dialect = OracleDialect.getInstance();
			} else if (url.contains("postgresql")) {
				dialect = PostgreSQLDialect.getInstance();
			}
			cacheSQLDialect(url, dialect);
		}
		if (dialect == null) {
			throw new NosuitableSQLDialectExeption("There is no suitable SQL dialect provide for url: " + url);
		}
		return dialect;
	}

}
