# Document
<p>http://doc.10mg.cn/sqltool</p>
<p>https://gitee.com/tenmg/sqltool/wikis</p>
<p>https://github.com/10MG/sqltool/wiki</p>

## About
Sqltool is a framework provides Dynamic Structured Query Language(DSQL) Parsing for distributed environment, such as spark, spring cloud, Dubbo and so on... Sqltool can help programmers manage and execute large scale and complex Dynamic Structured Query Language (DSQL), and make programmers free from manual splicing of complicated SQL work; Sqltool can also bring good news to programmers who use Spark SQL, because Dynamic Structured Query Language (DSQL) can be directly submitted to spark for execution and solve the problem of transferring Spark SQL parameters.

## ORM
Object relational mapping is a very important technology in Java language. Sqltool certainly supports simple but sufficient object relationship mapping technology to deal with many situations. For example, the query data is automatically converted into objects, and records are saved to the database through objects.

## Almost all kinds of database interactive interfaces
Sqltool provides database interaction interfaces that can cope with most business scenarios. It eliminates almost all of the JDBC code and manual setting of parameters and retrieval of results.
1.  Single value query

2.  Single entity object query

3.  Entity object list query

4.  Intelligent paging query

5.  Entity objects insert, update, merge witch is update if exists insert if none, or delete

6.  Entity objects soft update or merge witch is update if exists insert if none

## What is Dynamic Structured Query Language？

Dynamic Structured Query Language (DSQL) is a kind of Structured Query Language (SQL) which uses special character #[] to mark dynamic fragment. When the query is actually executed, whether the actual input parameter value is null determines whether to keep the fragment or not. At the same time, the special characters reserved in the fragment will be automatically removed. In order to avoid programmers manually splicing complicated SQL, programmers can be free from the complex business logic.

## Example
	SELECT
	  *
	FROM STAFF_INFO S
	WHERE S.STATUS = 'VALID'
	#[AND S.STAFF_ID = :staffId]
	#[AND S.STAFF_NAME LIKE :staffName]

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

## How to use
Here is a simple example to help you to get started.

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
	<dsqls xmlns="http://www.10mg.cn/schema/dsql"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.10mg.cn/schema/dsql http://www.10mg.cn/schema/dsql.xsd">
		<dsql id="find_staff_by_id">
			<script>
				<![CDATA[
					SELECT * FROM STAFF_INFO WHERE STAFF_ID = :staffId
				]]>
			</script>
		</dsql>
	</dsqls>

### Java

	/**
	 * Create Dao
	 */
	Dao dao = SqltoolFactory.createDao("sqltool.properties");

	StaffInfo june = new StaffInfo("000001");
	june.setStaffName("June");

	/**
	 * 
	 * Insert entity object/objects
	 */
	dao.insert(june);

	/**
	 * Load employee information with staffId
	 */
	StaffInfo params = new StaffInfo("000001");
	june = dao.get(params);

	/**
	 * Query with DSQL. We could use parameter name value pairs to specify parameters
	 */
	june = dao.get(StaffInfo.class, "SELECT * FROM STAFF_INFO S WHERE 1=1 #[AND S.STAFF_ID = :staffId]", "staffId","000001");

	/**
	 * Query with DSQL's id. You can also use map object to organize query parameters  at the same time
	 */
	Map<String, Object> parameters = new HashMap<String, Object>();
	parameters.put("staffId", "000001");
	june = dao.get(StaffInfo.class, "find_staff_by_id", parameters);

	/**
	 * 
	 * Save(insert or update) entity object/objects
	 */
	june.setStaffName("Happy June");
	dao.save(june);

