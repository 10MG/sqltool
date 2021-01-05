package cn.tenmg.sqltool;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import cn.tenmg.sqltool.factory.XMLFileSqltoolFactory;

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

	public static SqltoolContext initSqltoolContext() {
		/**
		 * 加载DSQL配置文件的基本包名
		 * 
		 * Base packages for where the configuration file is located
		 */
		String basePackages = "cn.tenmg.sqltool",
				/**
				 * DSQL配置文件的后缀名，默认“.dsql.xml”
				 * 
				 * The suffix of the configuration file, default '.dsql.xml'
				 */
				suffix = ".dsql.xml";
		/**
		 * 用于加载配置的Sqltool工厂
		 * 
		 * SqltoolFactory to load configuration
		 */
		SqltoolFactory sqltoolFactory = XMLFileSqltoolFactory.bind(basePackages, suffix);

		/**
		 * 日志中是否打印执行的SQL
		 * 
		 * Whether to print the executed SQL in the log
		 */
		boolean showSql = true;

		/**
		 * SqltoolContext理论上适合注入到任何分布式程序中，例如Spark
		 * 
		 * SqltoolContext is theoretically suitable for injection into any distributed
		 * program, such as spark
		 */
		SqltoolContext sqltoolContext = new SqltoolContext(sqltoolFactory, showSql, defaultBatchSize);
		return sqltoolContext;
	}

	public static void sqltoolContext(SqltoolContext sqltoolContext, Map<String, String> options) {
		// 测试插入数据
		insert(sqltoolContext, options);
		// 测试批量插入数据
		insertBatch(sqltoolContext, options);
		// 测试更新数据
		update(sqltoolContext, options);
		// 测试批量更新数据
		updateBatch(sqltoolContext, options);
		// 测试软保存数据
		save(sqltoolContext, options);
		// 测试批量软保存数据
		saveBatch(sqltoolContext, options);
		// 测试硬保存数据
		hardSave(sqltoolContext, options);
		// 测试批量硬保存数据
		hardSaveBatch(sqltoolContext, options);
		// 测试单条记录查询
		get(sqltoolContext, options);
		// 测试多条记录查询
		select(sqltoolContext, options);
		// 测试执行语句
		execute(sqltoolContext, options);
		// 测试执行更新语句
		executeUpdate(sqltoolContext, options);
	}

	private static void insert(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		String staffName = "June";
		StaffInfo staffInfo = new StaffInfo("000001");
		staffInfo.setStaffName(staffName);

		/**
		 * 
		 * 插入单条记录
		 * 
		 * Insert entity object/objects
		 */
		sqltoolContext.insert(options, staffInfo);

		staffInfo.setStaffName(null);
		sqltoolContext.save(options, staffInfo);

		/**
		 * 使用DSQL编号查询。同时，你还可以使用Map对象来更自由地组织查询参数
		 * 
		 * Query with DSQL's id. You can also use map object to organize query
		 * parameters at the same time
		 */
		Map<String, String> paramaters = new HashMap<String, String>();
		paramaters.put("staffId", "000001");
		StaffInfo june = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", paramaters);
		Assert.assertEquals(staffName, june.getStaffName());
		Assert.assertEquals(staffName, sqltoolContext.get(options, staffInfo).getStaffName());

		/**
		 * 插入多条记录
		 */
		// 条目数小于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		DecimalFormat df = new DecimalFormat("0000000000");
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		sqltoolContext.insert(options, staffInfos);
		Assert.assertEquals(defaultBatchSize - 1,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 条目数等于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		sqltoolContext.insert(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 条目数大于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		sqltoolContext.insert(options, staffInfos);
		Assert.assertEquals(defaultBatchSize + 1,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());
	}

	private static void insertBatch(SqltoolContext sqltoolContext, Map<String, String> options) {
		/**
		 * 批量插入多条记录
		 */
		// 条目数小于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		List<StaffInfo> staffInfos = new ArrayList<StaffInfo>();
		StaffInfo staffInfo;
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfo.setPosition(position);
			staffInfos.add(staffInfo);
		}
		sqltoolContext.insertBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize - 1,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 条目数等于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		sqltoolContext.insertBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 条目数大于批容量
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		sqltoolContext.insertBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize + 1,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());
	}

	private static void update(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
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
		sqltoolContext.insertBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 更新单条数据
		String staffId = df.format(0), staffName = "June";
		StaffInfo june = new StaffInfo(staffId), original = sqltoolContext.get(options, StaffInfo.class,
				"get_staff_info_by_staff_id", "staffId", staffId);
		june.setStaffName(staffName);
		june.setPosition(position);
		Assert.assertEquals(1, sqltoolContext.update(options, june));
		StaffInfo newStaffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId",
				staffId);
		Assert.assertEquals(staffName, newStaffInfo.getStaffName());
		Assert.assertEquals(original.getPosition(), newStaffInfo.getPosition());

		june.setStaffName(null);
		sqltoolContext.update(options, june);
		// 软更新，不更新null属性
		Assert.assertEquals(staffName, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());
		// 部分硬更新，属性即使是null也更新
		Assert.assertEquals(1, sqltoolContext.update(options, june, "staffName"));
		Assert.assertEquals(null, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		// 更新多条数据
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(staffName);
		}
		sqltoolContext.update(options, staffInfos);
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 软更新，不更新null属性
		long count = sqltoolContext.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName);
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(null);
		}
		sqltoolContext.update(options, staffInfos);
		Assert.assertEquals(count, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).longValue());
		
		// 部分硬更新，属性即使是null也更新
		sqltoolContext.update(options, staffInfos, "staffName");
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).longValue());

		// 尝试更新不存在的数据
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		Assert.assertEquals(0, sqltoolContext.update(options, staffInfo));
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		staffInfos.add(staffInfo);
		sqltoolContext.update(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());
	}

	private static void updateBatch(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
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
		sqltoolContext.insertBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());

		// 批量更新多条数据
		String staffName = "June";
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(staffName);
		}
		sqltoolContext.updateBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());
		
		// 软更新，不更新null属性
		for (int i = 0; i < defaultBatchSize; i++) {
			staffInfo = staffInfos.get(i);
			staffInfo.setStaffName(null);
		}
		sqltoolContext.updateBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());
		
		// 硬更新，null属性也更新
		sqltoolContext.updateBatch(options, staffInfos, "staffName");
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_the_same_name", "staffName", staffName).intValue());

		// 尝试更新不存在的数据
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		sqltoolContext.updateBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize,
				sqltoolContext.get(options, Long.class, "get_total_staff_count").intValue());
	}

	private static void save(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 
		 * 保存（插入或更新）实体对象
		 * 
		 * Save(insert or update) entity object/objects
		 */
		String staffId = "000001";
		StaffInfo june = new StaffInfo(staffId);
		june.setStaffName("June");
		sqltoolContext.save(options, june);
		StaffInfo staffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId",
				staffId);
		Assert.assertEquals(june.getStaffName(), staffInfo.getStaffName());

		june.setStaffName("Happy June");
		june.setPosition(position);
		sqltoolContext.save(options, june);
		staffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assert.assertEquals(june.getStaffName(), staffInfo.getStaffName());
		Assert.assertEquals(position, staffInfo.getPosition());

		june.setPosition(null);
		sqltoolContext.save(options, june, "position");
		Assert.assertEquals(null, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getPosition());

		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
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
		sqltoolContext.save(options, staffInfos, "position");
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		sqltoolContext.save(options, staffInfos, "position");
		Assert.assertEquals(1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfo.setPosition(position);
		staffInfos.add(staffInfo);
		sqltoolContext.save(options, staffInfos, "position");
		Assert.assertEquals(2, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
	}

	private static void saveBatch(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
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
		sqltoolContext.saveBatch(options, staffInfos, "position");
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		sqltoolContext.saveBatch(options, staffInfos, "position");
		Assert.assertEquals(1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		sqltoolContext.saveBatch(options, staffInfos, "position");
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		sqltoolContext.saveBatch(options, staffInfos, "position");
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assert.assertEquals(defaultBatchSize + 1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", null).intValue());

	}

	private static void hardSave(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		/**
		 * 硬保存数据
		 */
		String staffId = "000001";
		StaffInfo staffInfo = new StaffInfo(staffId);
		staffInfo.setStaffName("June");
		staffInfo.setPosition(position);
		sqltoolContext.hardSave(options, staffInfo);
		Assert.assertEquals(position,
				sqltoolContext
						.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffInfo.getStaffId())
						.getPosition());

		staffInfo.setPosition(null);
		sqltoolContext.hardSave(options, staffInfo);
		Assert.assertEquals(null,
				sqltoolContext
						.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffInfo.getStaffId())
						.getPosition());

		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
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
		sqltoolContext.hardSave(options, staffInfos);
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		sqltoolContext.hardSave(options, staffInfos);
		Assert.assertEquals(1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		sqltoolContext.hardSave(options, staffInfos);
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		sqltoolContext.hardSave(options, staffInfos);
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assert.assertEquals(defaultBatchSize + 1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", null).intValue());

	}

	private static void hardSaveBatch(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表

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
		sqltoolContext.hardSaveBatch(options, staffInfos);
		Assert.assertEquals(defaultBatchSize, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		// 条目数小于批容量
		staffInfos = new ArrayList<StaffInfo>();
		for (int i = 1; i < defaultBatchSize; i++) {
			staffInfo = new StaffInfo();
			staffInfo.setStaffId(df.format(i));
			staffInfo.setStaffName("" + i);
			staffInfos.add(staffInfo);
		}
		sqltoolContext.hardSaveBatch(options, staffInfos);
		Assert.assertEquals(1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数等于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize));
		staffInfo.setStaffName("" + defaultBatchSize);
		staffInfos.add(staffInfo);
		sqltoolContext.hardSaveBatch(options, staffInfos);
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());

		// 条目数大于批容量
		staffInfo = new StaffInfo();
		staffInfo.setStaffId(df.format(defaultBatchSize + 1));
		staffInfo.setStaffName("" + (defaultBatchSize + 1));
		staffInfos.add(staffInfo);
		sqltoolContext.hardSaveBatch(options, staffInfos);
		Assert.assertEquals(0, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", position).intValue());
		Assert.assertEquals(defaultBatchSize + 1, sqltoolContext
				.get(options, Long.class, "get_staff_count_of_specific_position", "position", null).intValue());
	}

	private static void get(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		String staffId = "000001", staffName = "June";
		StaffInfo june = new StaffInfo(staffId);
		june.setStaffName(staffName);
		sqltoolContext.save(options, june);

		/**
		 * 使用员工编号获取员工信息
		 * 
		 * Load staff information with staffId
		 */
		StaffInfo staffInfo = sqltoolContext.get(options, new StaffInfo(staffId));
		Assert.assertEquals(staffName, staffInfo.getStaffName());

		/**
		 * 使用DSQL编号查询
		 * 
		 * Query with id of DSQL's id
		 */
		staffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId);
		Assert.assertEquals(staffName, staffInfo.getStaffName());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffId", staffId);
		staffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", params);
		Assert.assertEquals(staffName, staffInfo.getStaffName());
	}

	private static void select(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表
		// 初始化数据
		StaffInfo sharry = new StaffInfo("000001"), june = new StaffInfo("000002");
		sharry.setStaffName("Sharry");
		june.setStaffName("June");
		sqltoolContext.save(options, Arrays.asList(sharry, june));

		/**
		 * 使用员工编号查询员工信息
		 * 
		 * Load staff information with staffId
		 */
		List<StaffInfo> staffInfos = sqltoolContext.select(options, new StaffInfo("000001"));
		Assert.assertNotNull(staffInfos);
		Assert.assertEquals(1, staffInfos.size());
		Assert.assertEquals("Sharry", staffInfos.get(0).getStaffName());

		/**
		 * 使用员工编号查询员工信息
		 */
		String[] staffIdArray = new String[] { "000001", "000002" };
		staffInfos = sqltoolContext.select(options, StaffInfo.class, "find_staff_info_by_staff_ids", "staffIds",
				staffIdArray);
		Assert.assertNotNull(staffInfos);
		Assert.assertEquals(2, staffInfos.size());
		Assert.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assert.assertEquals("June", staffInfos.get(1).getStaffName());

		List<String> staffIds = new ArrayList<String>();
		staffIds.add("000001");
		staffIds.add("000002");
		staffInfos = sqltoolContext.select(options, StaffInfo.class, "find_staff_info_by_staff_ids", "staffIds",
				staffIds);
		Assert.assertNotNull(staffInfos);
		Assert.assertEquals(2, staffInfos.size());
		Assert.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assert.assertEquals("June", staffInfos.get(1).getStaffName());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("staffIds", staffIdArray);
		staffInfos = sqltoolContext.select(options, StaffInfo.class, "find_staff_info_by_staff_ids", params);
		Assert.assertNotNull(staffInfos);
		Assert.assertEquals(2, staffInfos.size());
		Assert.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assert.assertEquals("June", staffInfos.get(1).getStaffName());

		params.put("staffIds", staffIds);
		staffInfos = sqltoolContext.select(options, StaffInfo.class, "find_staff_info_by_staff_ids", params);
		Assert.assertNotNull(staffInfos);
		Assert.assertEquals(2, staffInfos.size());
		Assert.assertEquals("Sharry", staffInfos.get(0).getStaffName());
		Assert.assertEquals("June", staffInfos.get(1).getStaffName());
	}

	private static void execute(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表

		/**
		 * 插入语句
		 */
		String staffId = "000001", staffName = "June";
		sqltoolContext.execute(options, "INSERT INTO STAFF_INFO(STAFF_ID, STAFF_NAME) VALUES (:staffId, :staffName)",
				"staffId", staffId, "staffName", staffName);
		StaffInfo staffInfo = sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId",
				staffId);
		Assert.assertEquals(staffName, staffInfo.getStaffName());

		/**
		 * 更新语句
		 */
		String newStaffName = "Sharry";
		sqltoolContext.execute(options, "UPDATE STAFF_INFO SET STAFF_NAME = :staffName WHERE STAFF_ID = :staffId",
				"staffId", staffId, "staffName", newStaffName);
		Assert.assertEquals(newStaffName, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 删除语句
		 */

		Map<String, String> params = new HashMap<String, String>();
		params.put("staffId", staffId);
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO WHERE STAFF_ID = :staffId", params);
		Assert.assertNull(
				sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId));
	}

	private static void executeUpdate(SqltoolContext sqltoolContext, Map<String, String> options) {
		sqltoolContext.execute(options, "DELETE FROM STAFF_INFO"); // 清空表

		/**
		 * 插入语句
		 */
		String staffId = "000001", staffName = "June";
		int count = sqltoolContext.executeUpdate(options,
				"INSERT INTO STAFF_INFO(STAFF_ID, STAFF_NAME) VALUES (:staffId, :staffName)", "staffId", staffId,
				"staffName", staffName);
		Assert.assertEquals(1, count);
		Assert.assertEquals(staffName, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 更新语句
		 */
		String newStaffName = "Sharry";
		count = sqltoolContext.executeUpdate(options,
				"UPDATE STAFF_INFO SET STAFF_NAME = :staffName WHERE STAFF_ID = :staffId", "staffId", staffId,
				"staffName", newStaffName);
		Assert.assertEquals(1, count);
		Assert.assertEquals(newStaffName, sqltoolContext
				.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId).getStaffName());

		/**
		 * 删除语句
		 */
		Map<String, String> params = new HashMap<String, String>();
		params.put("staffId", staffId);
		count = sqltoolContext.executeUpdate(options, "DELETE FROM STAFF_INFO WHERE STAFF_ID = :staffId", params);
		Assert.assertEquals(1, count);
		Assert.assertNull(
				sqltoolContext.get(options, StaffInfo.class, "get_staff_info_by_staff_id", "staffId", staffId));
	}

}
