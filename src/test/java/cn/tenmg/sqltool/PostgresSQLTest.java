package cn.tenmg.sqltool;

import org.junit.Test;

public class PostgresSQLTest {

	@Test
	public void doTest() {
		doTest(SqltoolFactory.createDao("postgresql.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao);
	}

}
