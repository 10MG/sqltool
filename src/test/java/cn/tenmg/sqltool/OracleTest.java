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
		TestUtils.sqltoolContext(sqltoolContext, initDataBaseOptions());
	}

	public static Map<String, String> initDataBaseOptions() {
		/**
		 * Oracle数据库配置项
		 * 
		 * Database options
		 */
		Map<String, String> oracle = new HashMap<String, String>();
		oracle.put("driver", "oracle.jdbc.OracleDriver");
		oracle.put("url", "jdbc:oracle:thin:@127.0.0.1:1521:orcl");
		oracle.put("user", "c##orcl");
		oracle.put("password", "orcl");
		return oracle;
	}
}
