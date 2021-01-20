# sqltool
## 关于（About）
Sqltool是一个给分布式环境提供动态结构化查询语言（DSQL）解析和执行的框架，如Spark、Spring Cloud、Dubbo等等。Sqltool能帮助程序员管理和执行庞大而复杂的动态结构化查询语言（DSQL），并使程序员从手动拼接繁杂的SQL工作中解脱；Sqltool还能给使用Spark SQL的程序员带来福音，因为动态结构化查询语言（DSQL）可以直接提交给Spark执行，解决Spark SQL传参的难题。

A framework provides Dynamic Structured Query Language(DSQL) Parsing for distributed environment, such as spark, spring cloud, Dubbo and so on... Sqltool can help programmers manage and execute large scale and complex Dynamic Structured Query Language (DSQL), and make programmers free from manual splicing of complicated SQL work; Sqltool can also bring good news to programmers who use Spark SQL, because Dynamic Structured Query Language (DSQL) can be directly submitted to spark for execution and solve the problem of transferring Spark SQL parameters.

## 什么是动态结构化查询语言（What is Dynamic Structured Query Language）？
动态结构化查询语言(DSQL)是一种使用特殊字符#[]标记动态片段的结构化查询语言(SQL)，当实际执行查询时，判断实际传入参数值是否为空（null）决定是否保留该片段，同时保留片段的特殊字符会被自动去除。以此来避免程序员手动拼接繁杂的SQL，使得程序员能从繁杂的业务逻辑中解脱出来。

Dynamic Structured Query Language (DSQL) is a kind of Structured Query Language (SQL) which uses special character #[] to mark dynamic fragment. When the query is actually executed, whether the actual input parameter value is null determines whether to keep the fragment or not. At the same time, the special characters reserved in the fragment will be automatically removed. In order to avoid programmers manually splicing complicated SQL, programmers can be free from the complex business logic.

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

1. When the parameter staffId is null and staffName is not null, the actual executed statement is as follows:

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_NAME LIKE :staffName
2. On the contrary, when the parameter staffName is null and staffId is not null, the actual executed statement is:

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_ID = :staffId
3. Or, when the parameters staffId and staffName are null, the actual executed statement is:

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
4. Finally, when the parameters staffId and staffName are not null, the actual executed statement is as follows：

		SELECT
		  *
		FROM STAFF_INFO S
		WHERE S.STATUS = 'VALID'
		AND S.STAFF_ID = :staffId
		AND S.STAFF_NAME LIKE :staffName
Through the above simple example, we can see the magic of Dynamic Structured Query Language (DSQL). The source of this magic is the clever use of a value: null, because the value is often rarely used in Structured Query Language (SQL), and even if used, it is often used as a special constant, such as: NVL(email, 'none'), WHERE EMAIL IS NOT NULL, etc.

## 如何使用（How to use)
以下是一个简单的例子可以帮你您起步。

Here is a simple example to help you get started.

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
	 * 加载DSQL配置文件的基本包名
	 * 
	 * Base packages for where the configuration file is located
	 */
	Dao dao = SqltoolFactory.createDao("sqltool.properties");

	StaffInfo june = new StaffInfo("000001");
	june.setStaffName("June");

	/**
	 * 
	 * 插入实体对象
	 * 
	 * Insert entity object/objects
	 */
	dao.insert(june);

	/**
	 * 使用员工编号加载员工信息
	 * 
	 * Load employee information with staffId
	 */
	StaffInfo params = new StaffInfo("000001");
	june = dao.get(params);

	/**
	 * 使用DSQL查询。可以使用参数名值对来指定参数
	 * 
	 * Query with DSQL. We could use parameter name value pairs to specify parameters
	 */
	june = dao.get(StaffInfo.class, "SELECT * FROM STAFF_INFO S WHERE 1=1 #[AND S.STAFF_ID = :staffId]", "staffId","000001");

	/**
	 * 使用DSQL编号查询。同时，你还可以使用Map对象来更自由地组织查询参数
	 * 
	 * Query with DSQL's id. You can also use map object to organize query parameters  at the same time
	 */
	Map<String, Object> paramaters = new HashMap<String, Object>();
	paramaters.put("staffId", "000001");
	june = dao.get(StaffInfo.class, "find_staff_by_id", paramaters);

	/**
	 * 
	 * 保存（插入或更新）实体对象
	 * 
	 * Save(insert or update) entity object/objects
	 */
	june.setStaffName("Happy June");
	dao.save(june);

## 对象关系映射（ORM）
对象关系映射在java语言中是一种非常重要的技术，sqltool当然支持简单但足以应对很多情况的对象关系映射技术。比如，将查询的数据自动转换为对象，通过对象保存记录到数据库中。

Object relational mapping is a very important technology in Java language. Sqltool certainly supports simple but sufficient object relationship mapping technology to deal with many situations. For example, the query data is automatically converted into objects, and records are saved to the database through objects.
## 提供了几乎你能想到的所有结构化数据库交互方法（It provides almost all kinds of structured database interaction methods you can imagine）

	/**
	 * 获取动态结构化查询语言工厂
	 * 
	 * @return 返回动态结构化查询语言工厂
	 */
	DSQLFactory getDSQLFactory();

	/**
	 * 获取默认数据源
	 * 
	 * @return 返回默认数据源
	 */
	DataSource getDefaultDataSource();

	/**
	 * 根据数据源名称获取数据源
	 * 
	 * @param name
	 *            数据源名称
	 * @return 如果指定名称的数据源存在则返回该数据源，否则返回null
	 */
	DataSource getDataSource(String name);

	/**
	 * 插入操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int insert(T obj);

	/**
	 * 插入操作
	 * 
	 * @param dataSource
	 *            数据源
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int insert(DataSource dataSource, T obj);

	/**
	 * 插入操作（实体对象集为空则直接返回null）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int insert(List<T> rows);

	/**
	 * 插入操作（实体对象集为空则直接返回null）
	 * 
	 * @param dataSource
	 *            数据源
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int insert(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量执行批量插入操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void insertBatch(List<T> rows);

	/**
	 * 使用默认批容量执行批量插入操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void insertBatch(DataSource dataSource, List<T> rows);

	/**
	 * 
	 * 批量插入操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void insertBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量插入操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void insertBatch(DataSource dataSource, List<T> rows, int batchSize);

	/**
	 * 软更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(T obj);

	/**
	 * 软更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(DataSource dataSource, T obj);

	/**
	 * 部分硬更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(T obj, String... hardFields);

	/**
	 * 部分硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象（不能为null）
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(DataSource dataSource, T obj, String... hardFields);

	/**
	 * 软更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(List<T> rows);

	/**
	 * 软更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(DataSource dataSource, List<T> rows);

	/**
	 * 部分硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(List<T> rows, String... hardFields);

	/**
	 * 部分硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬更新属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int update(DataSource dataSource, List<T> rows, String... hardFields);

	/**
	 * 使用默认批容量执行批量软更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void updateBatch(List<T> rows);

	/**
	 * 使用默认批容量执行批量软更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量执行批量部分硬更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬更新属性
	 */
	<T extends Serializable> void updateBatch(List<T> rows, String... hardFields);

	/**
	 * 使用默认批容量执行批量部分硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬更新属性
	 */
	<T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, String... hardFields);

	/**
	 * 
	 * 批量软更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void updateBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量软更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, int batchSize);

	/**
	 * 
	 * 批量部分硬更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 * @param hardFields
	 *            硬更新属性
	 */
	<T extends Serializable> void updateBatch(List<T> rows, int batchSize, String... hardFields);

	/**
	 * 
	 * 批量部分硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 * @param hardFields
	 *            硬更新属性
	 */
	<T extends Serializable> void updateBatch(DataSource dataSource, List<T> rows, int batchSize, String... hardFields);

	/**
	 * 硬更新操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardUpdate(T obj);

	/**
	 * 硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardUpdate(DataSource dataSource, T obj);

	/**
	 * 硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardUpdate(List<T> rows);

	/**
	 * 硬更新操作（实体对象集为空则直接返回0）
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardUpdate(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量执行批量硬更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void hardUpdateBatch(List<T> rows);

	/**
	 * 使用默认批容量执行批量硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void hardUpdateBatch(DataSource dataSource, List<T> rows);

	/**
	 * 
	 * 批量硬更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void hardUpdateBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量硬更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void hardUpdateBatch(DataSource dataSource, List<T> rows, int batchSize);

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(T obj);

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(DataSource dataSource, T obj);

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(T obj, String... hardFields);

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(DataSource dataSource, T obj, String... hardFields);

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(List<T> rows);

	/**
	 * 软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(DataSource dataSource, List<T> rows);

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(List<T> rows, String... hardFields);

	/**
	 * 部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int save(DataSource dataSource, List<T> rows, String... hardFields);

	/**
	 * 使用默认批容量批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void saveBatch(List<T> rows);

	/**
	 * 使用默认批容量批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 */
	<T extends Serializable> void saveBatch(List<T> rows, String... hardFields);

	/**
	 * 使用默认批容量批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 */
	<T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, String... hardFields);

	/**
	 * 
	 * 批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void saveBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量软保存。仅对属性值不为null的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, int batchSize);

	/**
	 * 
	 * 批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void saveBatch(List<T> rows, int batchSize, String... hardFields);

	/**
	 * 
	 * 批量部分硬保存。仅对属性值不为null或硬保存的字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param hardFields
	 *            硬保存属性
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void saveBatch(DataSource dataSource, List<T> rows, int batchSize, String... hardFields);

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardSave(T obj);

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardSave(DataSource dataSource, T obj);

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardSave(List<T> rows);

	/**
	 * 硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int hardSave(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void hardSaveBatch(List<T> rows);

	/**
	 * 使用默认批容量批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void hardSaveBatch(DataSource dataSource, List<T> rows);

	/**
	 * 
	 * 批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void hardSaveBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量硬保存。对所有字段执行插入/更新操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void hardSaveBatch(DataSource dataSource, List<T> rows, int batchSize);

	/**
	 * 从数据库查询并组装实体对象
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象
	 */
	<T extends Serializable> T get(T obj);

	/**
	 * 从数据库查询并组装实体对象
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象
	 */
	<T extends Serializable> T get(DataSource dataSource, T obj);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这事将返回结果集中的第1行第1列的值
	 * 
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 */
	<T extends Serializable> T get(Class<T> type, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这事将返回结果集中的第1行第1列的值
	 * 
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 */
	<T extends Serializable> T get(DataSource dataSource, Class<T> type, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行第1列的值
	 * 
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 */
	<T extends Serializable> T get(Class<T> type, String dsql, Map<String, ?> params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1行第1列的值
	 * 
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象
	 */
	<T extends Serializable> T get(DataSource dataSource, Class<T> type, String dsql, Map<String, ?> params);

	/**
	 * 从数据库查询并组装实体对象列表
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象列表
	 */
	<T extends Serializable> List<T> select(T obj);

	/**
	 * 从数据库查询并组装实体对象列表
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象
	 * @return 返回查询到的实体对象列表
	 */
	<T extends Serializable> List<T> select(DataSource dataSource, T obj);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
	 * 
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 */
	<T extends Serializable> List<T> select(Class<T> type, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
	 * 
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 */
	<T extends Serializable> List<T> select(DataSource dataSource, Class<T> type, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
	 * 
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 */
	<T extends Serializable> List<T> select(Class<T> type, String dsql, Map<String, ?> params);

	/**
	 * 使用动态结构化查询语言（DSQL）并组装对象列表，其中类型可以是实体对象，也可以是String、Number、
	 * Date、BigDecimal类型，这时将返回结果集中的第1列的值
	 * 
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            对象类型
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回查询到的对象列表
	 */
	<T extends Serializable> List<T> select(DataSource dataSource, Class<T> type, String dsql, Map<String, ?> params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定页码，指定页容量和指定参数（分别列出参数名和参数值）分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数（分别列出参数名和参数值）
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize, Object... params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定页码，指定页容量和指定参数（分别列出参数名和参数值）分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数（分别列出参数名和参数值）
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Object... params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定统计总数DSQL，指定页码，指定页容量和指定参数（分别列出参数名和参数值）分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param cntDsql
	 *            指定统计总数DSQL
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数（分别列出参数名和参数值）
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage, int pageSize,
			Object... params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定统计总数DSQL，指定页码，指定页容量和指定参数（分别列出参数名和参数值）分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param cntDsql
	 *            指定统计总数DSQL
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数（分别列出参数名和参数值）
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Object... params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定页码，指定页容量和指定参数分页查询对象。 该方法将根据DSQL中的别名将对象映射为指定类的对象，
	 * 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(Class<T> type, String dsql, long currentPage, int pageSize,
			Map<String, Object> params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定页码，指定页容量和指定参数分页查询对象。 该方法将根据DSQL中的别名将对象映射为指定类的对象，
	 * 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, long currentPage,
			int pageSize, Map<String, Object> params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定统计总数DSQL，指定页码，指定页容量和指定参数分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param cntDsql
	 *            指定统计总数DSQL
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(Class<T> type, String dsql, String cntDsql, long currentPage, int pageSize,
			Map<String, Object> params);

	/**
	 * 使用指定类，指定动态结构化查询语言（DSQL），指定统计总数DSQL，指定页码，指定页容量和指定参数分页查询对象。
	 * 该方法将根据DSQL中的别名将对象映射为指定类的对象， 需要保证DSQL中的别名和对象属性名保持一致。
	 * 
	 * @param <T>
	 *            实体类
	 * @param dataSource
	 *            数据源
	 * @param type
	 *            指定类
	 * @param dsql
	 *            指定动态结构化查询语言（DSQL）
	 * @param cntDsql
	 *            指定统计总数DSQL
	 * @param currentPage
	 *            指定页码
	 * @param pageSize
	 *            指定页容量
	 * @param params
	 *            指定参数
	 * @return 返回查询到的对象并封装为Page对象
	 */
	<T extends Serializable> Page<T> page(DataSource dataSource, Class<T> type, String dsql, String cntDsql,
			long currentPage, int pageSize, Map<String, Object> params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	boolean execute(String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	boolean execute(DataSource dataSource, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	boolean execute(String dsql, Map<String, ?> params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 如果第一个结果是ResultSet对象，则为true；如果第一个结果是更新计数或没有结果，则为false
	 */
	boolean execute(DataSource dataSource, String dsql, Map<String, ?> params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	int executeUpdate(String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	int executeUpdate(DataSource dataSource, String dsql, Object... params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	int executeUpdate(String dsql, Map<String, ?> params);

	/**
	 * 使用动态结构化查询语言（DSQL）执行插入、修改、删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数键值集
	 * @return 返回受影响行数
	 */
	int executeUpdate(DataSource dataSource, String dsql, Map<String, ?> params);

	/**
	 * 执行一个事务操作
	 * 
	 * @param transaction
	 *            事务对象
	 */
	void execute(Transaction transaction);

	/**
	 * 执行一个事务操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param transaction
	 *            事务对象
	 */
	void execute(DataSource dataSource, Transaction transaction);
