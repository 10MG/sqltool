# Sqltool

Sqltool是一个给分布式环境提供动态结构化查询语言（DSQL）解析和执行的框架，如Spark、Spring Cloud、Dubbo等等。Sqltool能帮助程序员管理和执行庞大而复杂的动态结构化查询语言（DSQL），并使程序员从手动拼接繁杂的SQL工作中解脱。

## DSQL

[DSQL](https://gitee.com/tenmg/dsql)的全称是动态结构化查询语言(Dynamic Structured Query Language)是一种使用特殊字符#[]标记动态片段的结构化查询语言(SQL)，当实际执行查询时，判断实际传入参数值是否为空（null）决定是否保留该片段，同时保留片段的特殊字符会被自动去除。以此来避免程序员手动拼接繁杂的SQL，使得程序员能从繁杂的业务逻辑中解脱出来。

### 例子

假设有如下动态查询语句：

```
SELECT
  *
FROM STAFF_INFO S
WHERE S.STATUS = 'VALID'
#[AND S.STAFF_ID = :staffId]
#[AND S.STAFF_NAME LIKE :staffName]
```

参数staffId为空（null），而staffName为非空（非null）时，实际执行的语句为：

```
SELECT
   *
 FROM STAFF_INFO S
 WHERE S.STATUS = 'VALID'
 AND S.STAFF_NAME LIKE :staffName
```

相反，参数staffName为空（null），而staffId为非空（非null）时，实际执行的语句为：


```
SELECT
   *
 FROM STAFF_INFO S
 WHERE S.STATUS = 'VALID'
 AND S.STAFF_ID = :staffId
```

或者，参数staffId、staffName均为空（null）时，实际执行的语句为：

```
SELECT
   *
 FROM STAFF_INFO S
 WHERE S.STATUS = 'VALID'
```

最后，参数staffId、staffName均为非空（非null）时，实际执行的语句为：

```
SELECT
   *
 FROM STAFF_INFO S
 WHERE S.STATUS = 'VALID'
 AND S.STAFF_ID = :staffId
 AND S.STAFF_NAME LIKE :staffName
```

通过上面这个小例子，我们看到了动态结构化查询语言（DSQL）的魔力。这种魔力的来源是巧妙的运用了一个值：空(null)，因为该值往往在结构化查询语言(SQL)中很少用到，而且即便使用也是往往作为特殊的常量使用，比如：
```
NVL(EMAIL,'无')
```
和
```
WHERE EMAIL IS NOT NULL
```
等等。

## 数据库

一些普通的查询、插入和全字段的硬更新API可以使用所有支持标准SQL的数据库，但部分ORM和分页查询等API需要依赖不同方言的实现类。

数据库     | 支持版本
-----------|---------
Mysql      | 1.0+
Oracle     | 1.1+
PostgreSQL | 1.1.1+
SQLServer  | 1.2.4+

## 连接池

1.2.0以下版本不支持数据库连接池，且API大不相同；1.2.0开始支持两种常用数据库连接池Druid和DBCP2；1.2.2及以上版本全面支持分布式环境下使用数据库连接池；1.2.3及以上版本，可以通过使用BasicDao自主配置数据源来使用数据库连接池或者不使用连接池（例如，直接使用MySQL启动程序的MysqlDataSource也是可行的）。

产品    | 支持版本
---|---
Druid     | 1.2+
DBCP2     | 1.2+
其他      | 1.2.3+

## 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

## 相关链接

开发文档： https://gitee.com/tenmg/sqltool/wikis

DSQL开源地址：https://gitee.com/tenmg/dsql

DSL开源地址：https://gitee.com/tenmg/dsl
