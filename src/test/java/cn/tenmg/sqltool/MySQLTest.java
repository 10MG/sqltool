package cn.tenmg.sqltool;

import org.junit.Test;

public class MySQLTest {

	@Test
	public void doTest() {
		doTest(SqltoolFactory.createDao("mysql.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao, true);
	}
}
