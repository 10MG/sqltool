package cn.tenmg.sqltool.sql;

import java.io.Serializable;

public interface SQLDialect extends Serializable {

	/**
	 * 获取软保存合并数据操作对象。软保存是只仅对属性值不为null的执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @return 返回软保存数据（插入或更新）操作对象
	 */
	<T> MergeSql save(Class<T> type);

	/**
	 * 获取部分硬保存合并数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据操作对象
	 */
	<T> MergeSql save(Class<T> type, String... hardFields);
	
	/**
	 * 获取硬保存合并数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体对象
	 * @return 返回获取硬保存数据操作对象
	 */
	<T> MergeSql hardSave(Class<T> type);

	/**
	 * 获取软保存数据（插入或更新）操作对象。软保存是只仅对属性值不为null的执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回软保存数据（插入或更新）操作对象
	 */
	<T> JdbcSql save(T obj);

	/**
	 * 
	 * 获取部分硬保存数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据操作对象
	 */
	<T> JdbcSql save(T obj, String... hardFields);

	/**
	 * 获取硬保存数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回获取硬保存数据操作对象
	 */
	<T> JdbcSql hardSave(T obj);
}
