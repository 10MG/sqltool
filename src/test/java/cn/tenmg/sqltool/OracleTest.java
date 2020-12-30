package cn.tenmg.sqltool;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class OracleTest {

	@Test
	public void doTest() {
		doTest(TestUtils.initSqltoolContext());
	}

	public static void doTest(SqltoolContext sqltoolContext) {
		TestUtils.sqltoolContext(sqltoolContext, initDatabaseOptions());
	}

	public static Map<String, String> initDatabaseOptions() {
		/**
		 * Oracle数据库配置项
		 * 
		 * Database options
		 */
		Map<String, String> options = new HashMap<String, String>();
		options.put("driver", "oracle.jdbc.OracleDriver");
		options.put("url", "jdbc:oracle:thin:@127.0.0.1:1521:orcl");
		options.put("user", "c##orcl");
		options.put("password", "orcl");
		return options;
	}
}