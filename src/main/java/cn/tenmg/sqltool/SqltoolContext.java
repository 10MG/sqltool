package cn.tenmg.sqltool;

import java.util.Properties;

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

	private static Properties defaultProperties, configProperties;

	static {
		try {
			defaultProperties = PropertiesLoaderUtils.loadFromClassPath(DEFAULT_STRATEGIES_PATH);
		} catch (Exception e) {
			defaultProperties = new Properties();
		}
		try {
			configProperties = PropertiesLoaderUtils.loadFromClassPath("sqltool-default.properties");
		} catch (Exception e) {
			configProperties = new Properties();
		}
		try {
			configProperties.putAll(PropertiesLoaderUtils
					.loadFromClassPath(defaultProperties.getProperty(CONFIG_LOCATION_KEY, DEFAULT_CONFIG_LOCATION)));
		} catch (Exception e) {
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
		return configProperties.containsKey(key) ? configProperties.getProperty(key)
				: defaultProperties.getProperty(key);
	}
}
