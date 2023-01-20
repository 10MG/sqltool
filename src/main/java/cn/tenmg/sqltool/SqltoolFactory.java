package cn.tenmg.sqltool;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import cn.tenmg.dsl.utils.PlaceHolderUtils;
import cn.tenmg.dsl.utils.PropertiesLoaderUtils;
import cn.tenmg.sqltool.exception.IllegalConfigException;

/**
 * Sqltool工厂
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
public abstract class SqltoolFactory {

	public static final String DAO_CONFIG_KEY = "sqltool.dao", DEFAULT_DAO = "cn.tenmg.sqltool.dao.BasicDao";

	/**
	 * 创建数据库访问对象
	 * 
	 * @param properties
	 *            配置属性
	 * @return 返回数据库访问对象
	 */
	public static Dao createDao(Properties properties) {
		try {
			String dao = properties.getProperty(DAO_CONFIG_KEY);
			if (dao == null) {
				dao = DEFAULT_DAO;
			}
			return (Dao) Class.forName(dao).getMethod("build", Properties.class).invoke(null, properties);
		} catch (Exception e) {
			throw new IllegalConfigException("Exception occurred when building database access object", e);
		}
	}

	/**
	 * 创建数据库访问对象
	 * 
	 * @param pathInClassPath
	 *            配置文件相对于classPath的路径
	 * 
	 * @return 返回数据库访问对象
	 */
	public static Dao createDao(String pathInClassPath) {
		Properties properties = new Properties();
		properties.putAll(System.getenv());// 系统环境变量
		properties.putAll(System.getProperties());// JVM环境变量
		try {
			PropertiesLoaderUtils.load(properties, pathInClassPath);
		} catch (Exception e) {
			throw new IllegalConfigException("Exception occurred when building database access object", e);
		}
		Object value;
		Entry<Object, Object> entry;
		for (Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			entry = it.next();
			value = entry.getValue();
			properties.put(entry.getKey(), PlaceHolderUtils.replace(value.toString(), properties));
		}
		return createDao(properties);
	}

}
