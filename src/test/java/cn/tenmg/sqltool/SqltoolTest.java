package cn.tenmg.sqltool;

import org.junit.Test;

public class SqltoolTest {

	@Test
	public void doTest() {
		/**
		 * 测试Mysql
		 */
		(new MySQLTest()).doTest();

		/**
		 * 测试Oracle
		 */
		(new OracleTest()).doTest();

		/**
		 * 测试PostgresSQL
		 */
		(new PostgresSQLTest()).doTest();
	}

}
