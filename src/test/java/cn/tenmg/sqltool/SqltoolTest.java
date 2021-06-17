package cn.tenmg.sqltool;

import org.junit.Test;

public class SqltoolTest {
	/**
	 * 测试Mysql
	 */
	@Test
	public void testMySQL() {
		(new MySQLTest()).doTest();
	}

	/**
	 * 测试Oracle
	 */
	@Test
	public void testOracle() {
		(new OracleTest()).doTest();
	}

	/**
	 * 测试PostgresSQL
	 */
	@Test
	public void testPostgresSQL() {
		(new PostgresSQLTest()).doTest();
	}

	/**
	 * 测试SQLServer
	 */
	@Test
	public void testSQLServer() {
		(new SQLServerTest()).doTest();
	}

}
