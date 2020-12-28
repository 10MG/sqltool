package cn.tenmg.sqltool;

import org.junit.Test;

public class SqltoolTest {

	@Test
	public void doTest() {
		SqltoolContext sqltoolContext = TestUtils.initSqltoolContext();
		/**
		 * 测试Mysql
		 */
		MySQLTest.doTest(sqltoolContext);

		/**
		 * 测试Oracle
		 */
		OracleTest.doTest(sqltoolContext);
	}

}
