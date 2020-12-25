package cn.tenmg.sqltool;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MysqlTest {

	@Test
	public void doTest() {
		doTest(TestUtils.initSqltoolContext());
	}

	public static void doTest(SqltoolContext sqltoolContext) {
		TestUtils.sqltoolContext(sqltoolContext, initDataBaseOptions());
	}

	public static Map<String, String> initDataBaseOptions() {
		/**
		 * Mysql数据库配置项
		 * 
		 * Database options
		 */
		Map<String, String> mysql = new HashMap<String, String>();
		mysql.put("driver", "com.mysql.cj.jdbc.Driver");
		mysql.put("url", "jdbc:mysql://127.0.0.1:3306/sqltool?useSSL=false&serverTimezone=Asia/Shanghai");
		mysql.put("user", "root");
		mysql.put("password", "");
		return mysql;
	}
}
