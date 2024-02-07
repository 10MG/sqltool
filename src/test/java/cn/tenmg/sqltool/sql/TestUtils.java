package cn.tenmg.sqltool.sql;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;

import cn.tenmg.sqltool.Dao;
import cn.tenmg.sqltool.data.Page;

public abstract class TestUtils {

	/**
	 * 
	 * 当不指定批处理大小时，默认的批处理大小
	 * 
	 * The default batch size when no batch size is specified
	 */
	private static int defaultBatchSize = 500;

	private static String position = "Software Engineer";

	private static DecimalFormat df = new DecimalFormat("0000000000");

	public static void testDao(Dao dao) {
		// 测试插入数据
		insert(dao);
		// 测试批量插入数据
		insertBatch(dao);
		// 测试更新数据
		update(dao);
		// 测试批量更新数据
		updateBatch(dao);
		// 测试软保存数据
		save(dao);
		// 测试批量软保存数据
		saveBatch(dao);
		// 测试硬保存数据
		hardSave(dao);
		// 测试批量硬保存数据
		hardSaveBatch(dao);
		// 测试删除数据
		delete(dao);
		// 测试删除数据
		deleteBatch(dao);
		// 测试单条记录查询
		get(dao);
		// 测试多条记录查询
		select(dao);
		// 测试分页查询
		page(dao);
		// 测试执行语句
		execute(dao);
		// 测试执行更新语句
		executeUpdate(dao);
	}

	private static void insert(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		String staffName = "June";
		StaffInfo staffInfo = new StaffInfo("000001");
		staffInfo.setStaffName(staffName);

		/**
		 * 
		 * 插入单条记录
		 * 
		 * Insert entity object/objects
		 */
		dao.insert(staffInfo);

		staffInfo.setStaffName(null);
		dao.save(staffInfo);

		/**
		 * 使用DSQL编号查询。同时，你还可以使用Map对象来更自由地组织查询参数
		 * 
		 * Query with DSQL's id. You can also use map object to organize query
		 * parameters at the same time
		 */
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("staffId", "000001");
		StaffInfo june = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", parameters);
		Assertions.assertEquals(staffName, june.getStaffName());
		Assertions.assertEquals(staffName, dao.get(staffInfo).getStaffName());

		/**
		 * 插入多条记录
		 */
		// 条目数小于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		DecimalFormat df = new DecimalFormat("0000000000");
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		dao.insert(staffInfos);
		Assertions.assertEquals(defaultBatchSize - 1, dao.get(Long.class, "get_total_staff_count").intValue());

		// 条目数等于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		dao.insert(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 条目数大于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		dao.insert(staffInfos);
		Assertions.assertEquals(defaultBatchSize + 1, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void insertBatch(Dao dao) {
		/**
		 * 批量插入多条记录
		 */
		// 条目数小于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.insertBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize - 1, dao.get(Long.class, "get_total_staff_count").intValue());

		// 条目数等于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		dao.insertBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 条目数大于批容量
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		dao.insertBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize + 1, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void update(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.insertBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 更新单条数据
		String staffId = df.format(0), staffName = "June";
		StaffInfo june = new StaffInfo(staffId),
				original = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		june.setStaffName(staffName);
		june.setPosition(position);
		Assertions.assertEquals(1, dao.update(june));
		StaffInfo newStaffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assertions.assertEquals(staffName, newStaffInfo.getStaffName());
		Assertions.assertEquals(original.getPosition(), newStaffInfo.getPosition());

		june.setStaffName(null);
		dao.update(june);
		// 软更新，不更新null属性
		Assertions.assertEquals(staffName,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());
		// 部分硬更新，属性即使是null也更新
		Assertions.assertEquals(1, dao.update(june, "staffName"));
		Assertions.assertEquals(null,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		// 更新多条数据
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(staffName);
		}
		dao.update(staffInfos);
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 软更新，不更新null属性
		long count = dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName);
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(null);
		}
		dao.update(staffInfos);
		Assertions.assertEquals(count,
				dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).longValue());

		// 部分硬更新，属性即使是null也更新
		dao.update(staffInfos, "staffName");
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).longValue());

		// 尝试更新不存在的数据
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		Assertions.assertEquals(0, dao.update(staffInfo));
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		staffInfos.add(staffInfo);
		dao.update(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void updateBatch(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.insertBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 批量更新多条数据
		String staffName = "June";
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(staffName);
		}
		dao.updateBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 软更新，不更新null属性
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(null);
		}
		dao.updateBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 硬更新，null属性也更新
		dao.updateBatch(staffInfos, "staffName");
		Assertions.assertEquals(0, dao.get(Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 尝试更新不存在的数据
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		dao.updateBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void save(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 
		 * 保存（插入或更新）实体对象
		 * 
		 * Save(insert or update) entity object/objects
		 */
		String staffId = "000001";
		StaffInfo june = new StaffInfo(staffId);
		june.setStaffName("June");
		dao.save(june);
		StaffInfo staffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assertions.assertEquals(june.getStaffName(), staffInfo.getStaffName());

		june.setStaffName("Happy June");
		june.setPosition(position);
		dao.save(june);
		staffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assertions.assertEquals(june.getStaffName(), staffInfo.getStaffName());
		Assertions.assertEquals(position, staffInfo.getPosition());

		june.setPosition(null);
		dao.save(june, "position");
		Assertions.assertEquals(null,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getPosition());

		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 软保存多条记录
		 */
		// 条目数小于批容量
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		dao.save(staffInfos, "position");
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		dao.save(staffInfos, "position");
		Assertions.assertEquals(1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		dao.save(staffInfos, "position");
		Assertions.assertEquals(2,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
	}

	private static void saveBatch(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 批量软保存多条记录
		 */
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 1; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.saveBatch(staffInfos, "position");
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		dao.saveBatch(staffInfos, "position");
		Assertions.assertEquals(1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		dao.saveBatch(staffInfos, "position");
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		dao.saveBatch(staffInfos, "position");
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assertions.assertEquals(defaultBatchSize + 1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", null).intValue());

	}

	private static void hardSave(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 硬保存数据
		 */
		String staffId = "000001";
		StaffInfo staffInfo = new StaffInfo(staffId);
		staffInfo.setStaffName("June");
		staffInfo.setPosition(position);
		dao.hardSave(staffInfo);
		Assertions.assertEquals(position, dao
				.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffInfo.getStaffId()).getPosition());

		staffInfo.setPosition(null);
		dao.hardSave(staffInfo);
		Assertions.assertEquals(null, dao
				.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffInfo.getStaffId()).getPosition());

		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 硬保存多条记录
		 */
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.hardSave(staffInfos);
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		dao.hardSave(staffInfos);
		Assertions.assertEquals(1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		dao.hardSave(staffInfos);
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		dao.hardSave(staffInfos);
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assertions.assertEquals(defaultBatchSize + 1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", null).intValue());

	}

	private static void hardSaveBatch(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		/**
		 * 批量硬保存多条记录
		 */
		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 1; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(defaultBatchSize,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(0,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assertions.assertEquals(defaultBatchSize + 1,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", null).intValue());
	}

	private static void delete(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		int total = defaultBatchSize * 3 + 1;
		for (int i = 1; i <= total; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(total,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 删除单条记录
		StaffInfo first = new StaffInfo(df.format(1));
		Assertions.assertEquals(1, dao.delete(first));
		Assertions.assertEquals(null, dao.get(first));

		// 删除多条记录
		// 小于批容量
		List<StaffInfo> staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 2; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize - 1, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(total - defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 等于批容量
		staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 1; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(defaultBatchSize + i));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(total - 2 * defaultBatchSize, dao.get(Long.class, "get_total_staff_count").intValue());

		// 大于批容量
		staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 0; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(2 * defaultBatchSize + i + 1));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize + 1, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(0, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void deleteBatch(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		int total = defaultBatchSize * 3;
		for (int i = 1; i <= total; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.hardSaveBatch(staffInfos);
		Assertions.assertEquals(total,
				dao.get(Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 删除多条记录
		// 小于批容量
		List<StaffInfo> staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize - 1, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(total - defaultBatchSize + 1, dao.get(Long.class, "get_total_staff_count").intValue());

		// 等于批容量
		staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(defaultBatchSize + i));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(total - 2 * defaultBatchSize + 1, dao.get(Long.class, "get_total_staff_count").intValue());

		// 大于批容量
		staffInfosForDelete = new ArrayList<StaffInfo>();
		for (int i = 0; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(2 * defaultBatchSize + i));
			staffInfosForDelete.add(staffInfo);
		}
		Assertions.assertEquals(defaultBatchSize + 1, dao.delete(staffInfosForDelete));
		Assertions.assertEquals(0, dao.get(Long.class, "get_total_staff_count").intValue());
	}

	private static void get(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		String staffId = "000001", staffName = "June";
		StaffInfo june = new StaffInfo(staffId);
		june.setStaffName(staffName);
		dao.save(june);

		/**
		 * 使用员工编号获取员工信息
		 * 
		 * Load staff information with staffId
		 */
		StaffInfo staffInfo = dao.get(new StaffInfo(staffId));
		Assertions.assertEquals(staffName, staffInfo.getStaffName());

		/**
		 * 使用DSQL编号查询
		 * 
		 * Query with id of DSQL's id
		 */
		staffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assertions.assertEquals(staffName, staffInfo.getStaffName());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffId", staffId);
		staffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", params);
		Assertions.assertEquals(staffName, staffInfo.getStaffName());
	}

	private static void select(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		StaffInfo sharry = new StaffInfo("000001"), june = new StaffInfo("000002");
		String staffName = "Sharry";
		sharry.setStaffName(staffName);
		june.setStaffName("June");
		dao.save(Arrays.asList(sharry, june));

		/**
		 * 使用员工编号查询员工信息
		 * 
		 * Load staff information with staffId
		 */
		List<StaffInfo> staffInfos = dao.select(new StaffInfo("000001"));
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(1, staffInfos.size());
		Assertions.assertEquals(staffName, staffInfos.get(0).getStaffName());

		StaffInfo staffInfo = new StaffInfo();
		staffInfo.setStaffName(staffName);
		staffInfos = dao.select(staffInfo);
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(1, staffInfos.size());
		Assertions.assertEquals(staffName, staffInfos.get(0).getStaffName());
		/**
		 * 使用员工编号查询员工信息
		 */
		String[] staffIdArray = new String[] { "000001", "000002" };
		staffInfos = dao.select(StaffInfo.class, "find_staff_info_by_staff_ids", "staffIds", staffIdArray);
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(2, staffInfos.size());
		Assertions.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assertions.assertEquals("June", staffInfos.get(1).getStaffName());

		List<String> staffIds = new ArrayList<String>();
		staffIds.add("000001");
		staffIds.add("000002");
		staffInfos = dao.select(StaffInfo.class, "find_staff_info_by_staff_ids", "staffIds", staffIds);
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(2, staffInfos.size());
		Assertions.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assertions.assertEquals("June", staffInfos.get(1).getStaffName());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffIds", staffIdArray);
		staffInfos = dao.select(StaffInfo.class, "find_staff_info_by_staff_ids", params);
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(2, staffInfos.size());
		Assertions.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assertions.assertEquals("June", staffInfos.get(1).getStaffName());

		params.put("staffIds", staffIds);
		staffInfos = dao.select(StaffInfo.class, "find_staff_info_by_staff_ids", params);
		Assertions.assertNotNull(staffInfos);
		Assertions.assertEquals(2, staffInfos.size());
		Assertions.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assertions.assertEquals("June", staffInfos.get(1).getStaffName());

		List<String> selectedStaffIds = dao.select(String.class, "find_staff_info_by_staff_ids", params);
		Assertions.assertLinesMatch(staffIds, selectedStaffIds);
	}

	private static void page(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		// 初始化数据
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		int staffNameLikeCount = 0;
		String staffNameLike = "1", staffId;
		for (int i = 1; i <= defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffId = df.format(i);
			if (staffId.contains(staffNameLike)) {
				staffNameLikeCount++;
			}
			staffInfo.setStaffId(staffId);
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		dao.save(staffInfos);

		long currentPage = 1;
		int pageSize = defaultBatchSize / 10;
		Page<StaffInfo> page = dao.page(StaffInfo.class, "select * from staff_info", currentPage, pageSize);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(defaultBatchSize, page.getTotal().intValue());
		Assertions.assertEquals(pageSize, page.getRows().size());

		long totalPage = page.getTotalPage();
		page = dao.page(StaffInfo.class, "select * from staff_info order by staff_id", totalPage, pageSize);
		Assertions.assertEquals(totalPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(defaultBatchSize, page.getTotal().intValue());
		List<StaffInfo> rows = page.getRows();
		Assertions.assertEquals(df.format(defaultBatchSize), rows.get(rows.size() - 1).getStaffId());

		page = dao.page(StaffInfo.class, "select * from staff_info order by staff_id desc", currentPage, pageSize);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(defaultBatchSize, page.getTotal().intValue());
		Assertions.assertEquals(df.format(defaultBatchSize), page.getRows().get(0).getStaffId());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffName", "1");
		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		
		params.put("staffName", "1");
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());

		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_with", currentPage, pageSize, "staffName", "1");
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_with", currentPage, pageSize, "staffName", "1");
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());

		params.put("defualtPosition", "Salesperson");
		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_order_by_has_param", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_order_by_has_param", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
		
		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_order_by_staff_name",
				"find_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_order_by_staff_name",
				"find_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());

		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_order_by_staff_name",
				"find_staff_info_staff_name_like", currentPage, pageSize, "staffName", "1");
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_order_by_staff_name",
				"find_staff_info_staff_name_like", currentPage, pageSize, "staffName", "1");
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());

		page = dao.page(StaffInfo.class, "find_staff_info_staff_name_like_order_by_staff_name",
				"find_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
		
		page = dao.page(StaffInfo.class, "page_staff_info_staff_name_like_order_by_staff_name",
				"page_staff_info_staff_name_like", currentPage, pageSize, params);
		Assertions.assertEquals(currentPage, page.getCurrentPage());
		Assertions.assertEquals(pageSize, page.getPageSize());
		Assertions.assertEquals(staffNameLikeCount, page.getTotal().intValue());
		Assertions.assertEquals(df.format(1), page.getRows().get(0).getStaffId());
	}

	private static void execute(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		/**
		 * 插入语句
		 */
		String staffId = "000001", staffName = "June";
		dao.execute("INSERT INTO STAFF_INFO(STAFF_ID, STAFF_NAME) VALUES (:staffId, :staffName)", "staffId", staffId,
				"staffName", staffName);
		StaffInfo staffInfo = dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assertions.assertEquals(staffName, staffInfo.getStaffName());

		/**
		 * 更新语句
		 */
		String newStaffName = "Sharry";
		dao.execute("UPDATE STAFF_INFO SET STAFF_NAME = :staffName WHERE STAFF_ID = :staffId", "staffId", staffId,
				"staffName", newStaffName);
		Assertions.assertEquals(newStaffName,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 删除语句
		 */

		Map<String, String> params = new HashMap<String, String>();
		params.put("staffId", staffId);
		dao.execute("DELETE FROM STAFF_INFO WHERE STAFF_ID = :staffId", params);
		Assertions.assertNull(dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId));
	}

	private static void executeUpdate(Dao dao) {
		dao.execute("DELETE FROM STAFF_INFO"); // 清空表

		/**
		 * 插入语句
		 */
		String staffId = "000001", staffName = "June";
		int count = dao.executeUpdate("INSERT INTO STAFF_INFO(STAFF_ID, STAFF_NAME) VALUES (:staffId, :staffName)",
				"staffId", staffId, "staffName", staffName);
		Assertions.assertEquals(1, count);
		Assertions.assertEquals(staffName,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 更新语句
		 */
		String newStaffName = "Sharry";
		count = dao.executeUpdate("UPDATE STAFF_INFO SET STAFF_NAME = :staffName WHERE STAFF_ID = :staffId", "staffId",
				staffId, "staffName", newStaffName);
		Assertions.assertEquals(1, count);
		Assertions.assertEquals(newStaffName,
				dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 删除语句
		 */
		Map<String, String> params = new HashMap<String, String>();
		params.put("staffId", staffId);
		count = dao.executeUpdate("DELETE FROM STAFF_INFO WHERE STAFF_ID = :staffId", params);
		Assertions.assertEquals(1, count);
		Assertions.assertNull(dao.get(StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId));
	}

}
