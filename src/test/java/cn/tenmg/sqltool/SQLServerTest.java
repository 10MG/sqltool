package cn.tenmg.sqltool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import cn.tenmg.sqltool.data.Page;

public class SQLServerTest {

	private static DecimalFormat df = new DecimalFormat("0000000000");

	private static String position = "Software Engineer";

	@Test
	public void doTest() {
		doTest(SqltoolFactory.createDao("sqlserver.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao);

		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		String staffId;
		int countLike1 = 0;
		for (int i = 1; i <= 1000; i++) {
			staffInfo = new StaffInfo();
			staffId = df.format(i);
			staffInfo.setStaffId(staffId);
			String staffName = "" + i;
			staffInfo.setStaffName(staffName);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
			if (staffName.contains("1")) {
				countLike1++;
			}
		}
		dao.save(staffInfos);

		long currentPage = 1;
		int pageSize = 10;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffName", "1");
		params.put("offset", pageSize);

		Page<StaffInfo> page = dao.page(StaffInfo.class,
				"SELECT staff_id, staff_name FROM STAFF_INFO WHERE STAFF_NAME LIKE :staffName ORDER BY STAFF_NAME OFFSET 0 ROW FETCH NEXT :offset ROW ONLY",
				currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class,
				" SELECT staff_id, staff_name FROM STAFF_INFO WHERE STAFF_NAME LIKE :staffName ORDER BY STAFF_NAME OFFSET 0 ROW FETCH NEXT :offset ROW ONLY",
				currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class,
				"SELECT staff_id, staff_name FROM STAFF_INFO WHERE STAFF_NAME LIKE :staffName ORDER BY STAFF_NAME OFFSET 0 ROW FETCH NEXT :offset ROW ONLY ",
				currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class, "sqlserver_offset_fetch", currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class,
				"SELECT staff_id, staff_name FROM STAFF_INFO WHERE STAFF_NAME LIKE :staffName ORDER BY STAFF_NAME OFFSET 10 ROW",
				currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class,
				" SELECT staff_id, staff_name FROM STAFF_INFO WHERE STAFF_NAME LIKE :staffName ORDER BY STAFF_NAME OFFSET 10 ROW",
				currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class, "sqlserver_offset", currentPage, pageSize, params);
		Assert.assertEquals(currentPage, page.getCurrentPage());
		Assert.assertEquals(pageSize, page.getPageSize());
		Assert.assertTrue(page.getTotal().intValue() == countLike1 - (int) params.get("offset"));
	}

}
