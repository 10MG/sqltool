package cn.tenmg.sqltool;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import cn.tenmg.dsl.utils.PlaceHolderUtils;
import cn.tenmg.dsl.utils.PropertiesLoaderUtils;

/**
 * Sqltool上下文
 * 
 * @author June wjzho@aliyun.com
 * 
 * @since 1.4.3
 */
public abstract class SqltoolContext {

	private static final String DEFAULT_STRATEGIES_PATH = "sqltool-context-loader.properties",
			CONFIG_LOCATION_KEY = "config.location", DEFAULT_CONFIG_LOCATION = "sqltool-context.properties";

	private static Properties config = new Properties();

	static {
		config.putAll(System.getenv());// 系统环境变量
		config.putAll(System.getProperties());// JVM环境变量
		PropertiesLoaderUtils.loadIgnoreException(config, DEFAULT_STRATEGIES_PATH);
		PropertiesLoaderUtils.loadIgnoreException(config, "sqltool-default.properties");
		PropertiesLoaderUtils.loadIgnoreException(config,
				config.getProperty(CONFIG_LOCATION_KEY, DEFAULT_CONFIG_LOCATION));
		Object value;
		Entry<Object, Object> entry;
		for (Iterator<Entry<Object, Object>> it = config.entrySet().iterator(); it.hasNext();) {
			entry = it.next();
			value = entry.getValue();
			config.put(entry.getKey(), PlaceHolderUtils.replace(value.toString(), config));
		}
	}

	/**
	 * 获取配置文件所在位置
	 * 
	 * @return 配置文件所在位置
	 */
	public static String getConfigLocation() {
		return getProperty(CONFIG_LOCATION_KEY);
	}

	/**
	 * 根据键获取配置的属性。优先查找用户配置属性，如果用户配置属性不存在从上下文配置中查找
	 * 
	 * @param key
	 *            键
	 * @return 配置属性值或null
	 */
	public static String getProperty(String key) {
		return config.getProperty(key);
	}

}
