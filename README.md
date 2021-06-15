# 文档
<p>http://doc.10mg.cn/sqltool</p>
<p>https://gitee.com/tenmg/sqltool/wikis</p>
<p>https://github.com/10MG/sqltool/wiki</p>

## 关于
Sqltool是一个给分布式环境提供动态结构化查询语言（DSQL）解析和执行的框架，如Spark、Spring Cloud、Dubbo等等。Sqltool能帮助程序员管理和执行庞大而复杂的动态结构化查询语言（DSQL），并使程序员从手动拼接繁杂的SQL工作中解脱；Sqltool还能给使用Spark SQL的程序员带来福音，因为动态结构化查询语言（DSQL）可以直接提交给Spark执行，解决Spark SQL传参的难题。

## 什么是动态结构化查询语言？
动态结构化查询语言(DSQL)是一种使用特殊字符#[]标记动态片段的结构化查询语言(SQL)，当实际执行查询时，判断实际传入参数值是否为空（null）决定是否保留该片段，同时保留片段的特殊字符会被自动去除。以此来避免程序员手动拼接繁杂的SQL，使得程序员能从繁杂的业务逻辑中解脱出来。

## 例子（Example）
	SELECT
	  *
	FROM STAFF_INFO S
	WHERE S.STATUS = 'VALID'
	#[AND S.STAFF_ID = :staffId]
	#[AND S.STAFF_NAME LIKE :staffName]
1. 参数staffId为空（null），而staffName为非空（非null）时，实际执行的语句为：

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_NAME LIKE :staffName
2. 相反，参数staffName为空（null），而staffId为非空（非null）时，实际执行的语句为：

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_ID = :staffId
3. 或者，参数staffId、staffName均为空（null）时，实际执行的语句为：

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
4. 最后，参数staffId、staffName均为非空（非null）时，实际执行的语句为：

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_ID = :staffId
		AND S.STAFF_NAME LIKE :staffName
通过上面这个小例子，我们看到了动态结构化查询语言（DSQL）的魔力。这种魔力的来源是巧妙的运用了一个值：空(null)，因为该值往往在结构化查询语言(SQL)中很少用到，而且即使使用也是往往作为特殊的常量使用，比如：NVL(EMAIL,'无')，WHERE EMAIL IS NOT NULL等。

## 如何使用（How to use)
以下是一个简单的例子可以帮你您起步。

### sqltool.properties
	# Packages to scan DSQL configuration file
	sqltool.basePackages=cn.tenmg.sqltool
	# Suffix of DSQL configuration file
	sqltool.suffix=.dsql.xml
	# Show SQL when execute them
	sqltool.showSql=true
	# Type of Dao, default cn.tenmg.sqltool.dao.BasicDao
	sqltool.dao=cn.tenmg.sqltool.dao.DistributedDao
	# Use driud database connection pool(It's also default)
	sqltool.datasource.type=com.alibaba.druid.pool.DruidDataSource
	sqltool.datasource.driverClassName=com.mysql.cj.jdbc.Driver
	sqltool.datasource.url=jdbc:mysql://127.0.0.1:3306/sqltool?useSSL=false&serverTimezone=Asia/Shanghai
	sqltool.datasource.username=root
	sqltool.datasource.password=
	sqltool.datasource.maxWait=60000
	sqltool.datasource.minIdle=5
	sqltool.datasource.maxActive=10
	sqltool.datasource.initialSize=5
	sqltool.datasource.testOnBorrow=true
	sqltool.datasource.testOnReturn=false
	sqltool.datasource.testWhileIdle=true
	sqltool.datasource.validationQuery=select 1
	sqltool.datasource.validationQueryTimeout=30000
	sqltool.datasource.timeBetweenEvictionRunsMillis=600000
	sqltool.datasource.minEvictableIdleTimeMillis=300000
	sqltool.datasource.maxEvictableIdleTimeMillis=3600000
	sqltool.datasource.poolPreparedStatements=true
	sqltool.datasource.maxOpenPreparedStatements=20

### staff-info.dsql.xml
	<?xml version="1.0" encoding="utf-8"?>
	<sqltool xmlns="http://www.10mg.cn/schema/sqltool"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.10mg.cn/schema/sqltool http://www.10mg.cn/schema/sqltool.xsd">
		<dsql id="find_staff_by_id">
			<script>
				<![CDATA[
					SELECT * FROM STAFF_INFO WHERE STAFF_ID = :staffId
				]]>
			</script>
		</dsql>
	</sqltool>

### Java

	/**
	 * 创建数据库访问对象
	 */
	Dao dao = SqltoolFactory.createDao("sqltool.properties");

	StaffInfo june = new StaffInfo("000001");
	june.setStaffName("June");

	/**
	 * 
	 * 插入实体对象
	 */
	dao.insert(june);

	/**
	 * 使用员工编号加载员工信息
	 */
	StaffInfo params = new StaffInfo("000001");
	june = dao.get(params);

	/**
	 * 使用DSQL查询。可以使用参数名值对来指定参数
	 */
	june = dao.get(StaffInfo.class, "SELECT * FROM STAFF_INFO S WHERE 1=1 #[AND S.STAFF_ID = :staffId]", "staffId","000001");

	/**
	 * 使用DSQL编号查询。同时，你还可以使用Map对象来更自由地组织查询参数
	 */
	Map<String, Object> paramaters = new HashMap<String, Object>();
	paramaters.put("staffId", "000001");
	june = dao.get(StaffInfo.class, "find_staff_by_id", paramaters);

	/**
	 * 
	 * 保存（插入或更新）实体对象
	 */
	june.setStaffName("Happy June");
	dao.save(june);

## 对象关系映射（ORM）
对象关系映射在java语言中是一种非常重要的技术，sqltool当然支持简单但足以应对很多情况的对象关系映射技术。比如，将查询的数据自动转换为对象，通过对象保存记录到数据库中。

## 完善的数据库交互接口

1.  单值查询

2.  单个实体对象查询

3.  实体对象列表查询

4.  智能分页查询

5.  实体对象插入、更新、合并（有则更新、无则插入）、删除

6.  实体对象软更新、合并（有则更新、无则插入）
