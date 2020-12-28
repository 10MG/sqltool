package cn.tenmg.sqltool;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MySQLTest {

	@Test
	public void doTest() {
		doTest(TestUtils.initSqltoolContext());
	}

	public static void doTest(SqltoolContext sqltoolContext) {
		TestUtils.sqltoolContext(sqltoolContext, initDatabaseOptions());
	}

	public static Map<String, String> initDatabaseOptions() {
		/**
		 * Mysql数据库配置项
		 * 
		 * Database options
		 */
		Map<String, String> options = new HashMap<String, String>();
		options.put("driver", "com.mysql.cj.jdbc.Driver");
		options.put("url", "jdbc:mysql://127.0.0.1:3306/sqltool?useSSL=false&serverTimezone=Asia/Shanghai");
		options.put("user", "root");
		options.put("password", "");
		return options;
	}
}
