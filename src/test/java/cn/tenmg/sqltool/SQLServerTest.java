package cn.tenmg.sqltool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.tenmg.sqltool.data.Page;

public class SQLServerTest {

	private static DecimalFormat df = new DecimalFormat("0000000000");

	private static String position = "Software Engineer";

	@Test
	public void testBasicDao() {
		doTest(SqltoolFactory.createDao("sqlserver.properties"));
	}

	@Test
	public void testDistributedDao() {
		doTest(SqltoolFactory.createDao("sqlserver2.properties"));
	}

	public static void doTest(Dao dao) {
		TestUtils.testDao(dao);

		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		String staffId;
		long countLike1 = 0;
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

		long currentPage = 1, rows = 10;
		int pageSize = 10;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffName", "1");
		params.put("rows", rows);

		Page<StaffInfo> page = dao.page(StaffInfo.class, "find_offset_fetch", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		if (countLike1 >= rows) {
			Assertions.assertEquals(rows, page.getTotal());
		} else {
			Assertions.assertEquals(countLike1, page.getTotal());
		}

		page = dao.page(StaffInfo.class, "page_offset_fetch", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		if (countLike1 >= rows) {
			Assertions.assertEquals(rows, page.getTotal());
		} else {
			Assertions.assertEquals(countLike1, page.getTotal());
		}

		long offset = 10;
		params.put("offset", offset);
		page = dao.page(StaffInfo.class, "find_offset", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		if (countLike1 >= offset) {
			Assertions.assertEquals(countLike1 - offset, page.getTotal());
		} else {
			Assertions.assertEquals(0L, page.getTotal());
		}

		page = dao.page(StaffInfo.class, "page_offset", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		if (countLike1 >= offset) {
			Assertions.assertEquals(countLike1 - offset, page.getTotal());
		} else {
			Assertions.assertEquals(0L, page.getTotal());
		}
	}

}
