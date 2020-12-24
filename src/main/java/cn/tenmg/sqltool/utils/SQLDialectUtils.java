package cn.tenmg.sqltool.utils;

import java.util.Map;

import cn.tenmg.sqltool.exception.NosuitableSQLDialectExeption;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.dialect.MySQLDialect;
import cn.tenmg.sqltool.sql.dialect.OracleDialect;

/**
 * 方言工具类
 * 
 * @author 赵伟均
 *
 */
public class SQLDialectUtils {

	public static SQLDialect getSQLDialect(Map<String, String> options) {
		String url = options.get("url");
		if (url != null) {
			if (url.contains("mysql")) {
				return MySQLDialect.getInstance();
			} else if (url.contains("oracle")) {
				return OracleDialect.getInstance();
			}
		}
		throw new NosuitableSQLDialectExeption("There is no suitable SQL dialect provide for url: " + url);
	}

}
