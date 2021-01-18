package cn.tenmg.sqltool;

import org.junit.Test;

public class OracleTest {

	@Test
	public void doTest() {
		doTest(SqltoolFactory.createDao("mysql.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao);
	}
}
