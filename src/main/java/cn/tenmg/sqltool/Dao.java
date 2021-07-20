package cn.tenmg.sqltool;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.tenmg.dsql.DSQLFactory;
import cn.tenmg.sqltool.data.Page;

/**
 * 数据库访问对象
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.2.0
 */
public interface Dao {

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
	 * 删除操作
	 * 
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int delete(T obj);

	/**
	 * 删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param obj
	 *            实体对象（不能为null）
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int delete(DataSource dataSource, T obj);

	/**
	 * 删除操作（实体对象集为空则直接返回0）
	 * 
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int delete(List<T> rows);

	/**
	 * 删除操作（实体对象集为空则直接返回0）
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @return 返回受影响行数
	 */
	<T extends Serializable> int delete(DataSource dataSource, List<T> rows);

	/**
	 * 使用默认批容量执行批量删除操作
	 * 
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void deleteBatch(List<T> rows);

	/**
	 * 使用默认批容量执行批量删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 */
	<T extends Serializable> void deleteBatch(DataSource dataSource, List<T> rows);

	/**
	 * 
	 * 批量删除操作
	 * 
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void deleteBatch(List<T> rows, int batchSize);

	/**
	 * 
	 * 批量删除操作
	 * 
	 * @param dataSource
	 *            数据源
	 * @param rows
	 *            实体对象集
	 * @param batchSize
	 *            批容量
	 */
	<T extends Serializable> void deleteBatch(DataSource dataSource, List<T> rows, int batchSize);

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

}
