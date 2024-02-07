package cn.tenmg.sqltool.sql.dialect;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import cn.tenmg.sqltool.sql.SQLDialectTest;

public class AllSQLDialectTest {
	@Test
	public void testAll() {
		SQLDialectTest test;
		ServiceLoader<SQLDialectTest> loader = ServiceLoader.load(SQLDialectTest.class);
		for (Iterator<SQLDialectTest> it = loader.iterator(); it.hasNext();) {
			test = it.next();
			test.testBasicDaoWithDBCP2();
			test.testBasicDaoWithDruid();
			test.testDistributedDaoWithDBCP2();
			test.testDistributedDaoWithDruid();
		}
	}
}
