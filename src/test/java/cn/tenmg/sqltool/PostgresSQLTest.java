package cn.tenmg.sqltool;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PostgresSQLTest {

	@Test
	public void doTest() {
		doTest(TestUtils.initSqltoolContext());
	}

	public static void doTest(SqltoolContext sqltoolContext) {
		TestUtils.sqltoolContext(sqltoolContext, initDatabaseOptions());
	}

	public static Map<String, String> initDatabaseOptions() {
		/**
		 * PostgresSQL数据库配置项
		 * 
		 * Database options
		 */
		Map<String, String> options = new HashMap<String, String>();
		options.put("driver", "org.postgresql.Driver");
		options.put("url", "jdbc:postgresql://localhost:5432/sqltool");
		options.put("user", "postgres");
		options.put("password", "postgres");
		return options;
	}
}
