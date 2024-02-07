package cn.tenmg.sqltool.sql;

public interface SQLDialectTest {

	/**
	 * 使用 DBCP2 连接池测试 BasicDao
	 */
	void testBasicDaoWithDBCP2();

	/**
	 * 使用 Druid 连接池测试 BasicDao
	 */
	void testBasicDaoWithDruid();

	/**
	 * 使用 DBCP2 连接池测试 DistributedDao
	 */
	void testDistributedDaoWithDBCP2();

	/**
	 * 使用 Druid 连接池测试 DistributedDao
	 */
	void testDistributedDaoWithDruid();

}
