package cn.tenmg.sqltool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.tenmg.sqltool.data.Page;

public class MySQLTest {

	private static DecimalFormat df = new DecimalFormat("0000000000");

	private static String position = "Software Engineer";

	@Test
	public void testBasicDao() {
		doTest(SqltoolFactory.createDao("mysql.properties"));
	}

	@Test
	public void testDistributedDao() {
		doTest(SqltoolFactory.createDao("mysql2.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao);

		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		String staffId;
		for (int i = 1; i <= 1000; i++) {
			staffInfo = new StaffInfo();
			staffId = df.format(i);
			staffInfo.setStaffId(staffId);
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.save(staffInfos);

		long currentPage = 1;
		int pageSize = 10;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffName", "1");
		params.put("limit", pageSize);
		Page<StaffInfo> page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_limit", currentPage, pageSize,
				params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertTrue(pageSize >= page.getTotal().intValue());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_limit", currentPage, pageSize,
				params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertTrue(pageSize >= page.getTotal().intValue());

		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_limit", currentPage, pageSize, "staffName",
				"1", "limit", pageSize);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertTrue(pageSize >= page.getTotal().intValue());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_limit", currentPage, pageSize, "staffName",
				"1", "limit", pageSize);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertTrue(pageSize >= page.getTotal().intValue());
	}
}
