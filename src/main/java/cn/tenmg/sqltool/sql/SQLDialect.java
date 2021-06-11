package cn.tenmg.sqltool.sql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * SQL方言
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface SQLDialect extends Serializable {

	/**
	 * 获取软更新的更新数据操作对象。软更新是只仅对属性值不为null的执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @return 返回软保存数据（插入或更新）操作对象
	 */
	<T> UpdateSQL update(Class<T> type);

	/**
	 * 获取部分硬更新的更新数据操作对象。软更新是只仅对属性值不为null的执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据操作对象
	 */
	<T> UpdateSQL update(Class<T> type, String... hardFields);

	/**
	 * 获取软保存合并数据操作对象。软保存是只仅对属性值不为null的执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @return 返回软保存数据（插入或更新）操作对象
	 */
	<T> MergeSQL save(Class<T> type);

	/**
	 * 获取部分硬保存合并数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体类型
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据操作对象
	 */
	<T> MergeSQL save(Class<T> type, String... hardFields);

	/**
	 * 获取硬保存合并数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作，合并数据是指当数据不存在时执行插入操作，已存在时执行更新操作。
	 * 
	 * @param type
	 *            实体对象
	 * @return 返回获取硬保存数据操作对象
	 */
	<T> MergeSQL hardSave(Class<T> type);

	/**
	 * 获取软更新的更新数据操作对象。软更新是只仅对属性值不为null的执行更新操作。
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回软保存数据（插入或更新）SQL对象
	 */
	<T> SQL update(T obj);

	/**
	 * 获取部分硬更新的更新数据操作对象。软更新是只仅对属性值不为null的执行更新操作。
	 * 
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据SQL对象
	 */
	<T> SQL update(T obj, String... hardFields);

	/**
	 * 获取软保存数据（插入或更新）操作对象。软保存是只仅对属性值不为null的执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回软保存数据（插入或更新）SQL对象
	 */
	<T> SQL save(T obj);

	/**
	 * 
	 * 获取部分硬保存数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @param hardFields
	 *            硬保存字段名
	 * @return 返回部分硬保存数据SQL对象
	 */
	<T> SQL save(T obj, String... hardFields);

	/**
	 * 获取硬保存数据操作对象。硬保存是指无论属性值是不是null均会执行保存操作
	 * 
	 * @param obj
	 *            实体对象
	 * @return 返回获取硬保存数据SQL对象
	 */
	<T> SQL hardSave(T obj);

	/**
	 * 根据SQL生成特定数据库统计总记录数SQL
	 * 
	 * @param sql
	 *            查询SQL
	 * @param sqlMetaData
	 *            SQL相关数据
	 * @return 返回查询总记录数的SQL
	 */
	String countSql(String sql, SQLMetaData sqlMetaData);

	/**
	 * 根据SQL、页容量pageSize和当前页码currentPage生成特定数据库的分页查询SQL
	 * 
	 * @param con
	 *            已开启的数据库连接
	 * @param sql
	 *            SQL
	 * @param params
	 *            查询参数集
	 * @param sqlMetaData
	 *            SQL相关数据
	 * @param pageSize
	 *            页容量
	 * @param currentPage
	 *            当前页码
	 * @return 返回分页查询SQL
	 * @throws SQLException
	 *             SQL异常
	 */
	String pageSql(Connection con, String sql, List<Object> params, SQLMetaData sqlMetaData, int pageSize,
			long currentPage) throws SQLException;
}
