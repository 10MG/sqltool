package cn.tenmg.sqltool.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import cn.tenmg.sqltool.datasource.DataSourceFactory;
import cn.tenmg.sqltool.exception.IllegalConfigException;

public abstract class DatabaseConnectionPool {

	private static final String DATASOURCE_PREFIX = "sqltool.datasource.", DEFAULT_NAME = "default",
			DATASOURCE_REGEX = "^".concat(DATASOURCE_PREFIX.replaceAll("\\.", "\\\\."))
					.concat("([\\S]+\\.){0,1}[^\\.]+$");

	private static final int DATASOURCE_PREFIX_LEN = DATASOURCE_PREFIX.length();

	private static DataSource defaultDataSource;

	private static final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

	/**
	 * 初始化数据库连接池
	 * 
	 * @param properties
	 *            数据库连接池配置
	 * @throws Exception
	 *             初始化发生异常
	 */
	public static void init(Properties properties) throws Exception {
		Map<String, Properties> datasourceConfigs = new HashMap<String, Properties>();
		String key, name, param, firstName = null;
		Object value;
		Properties datasourceConfig;
		for (Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();
			key = entry.getKey().toString();
			value = entry.getValue();
			if (key.matches(DATASOURCE_REGEX)) {
				param = key.substring(DATASOURCE_PREFIX_LEN);
				int index = param.indexOf(".");
				if (index > 0) {
					name = param.substring(0, index);
					param = param.substring(index);
				} else {
					name = DEFAULT_NAME;
				}
				if (firstName == null) {
					firstName = name;
				}
				datasourceConfig = datasourceConfigs.get(name);
				if (datasourceConfig == null) {
					datasourceConfig = new Properties();
					datasourceConfigs.put(name, datasourceConfig);
				}
				datasourceConfig.put(param, value);
			}
		}
		if (CollectionUtils.isEmpty(datasourceConfigs)) {
			throw new IllegalConfigException("No datasource is configured, please check the configuration");
		}
		String defaultName = DEFAULT_NAME;
		datasourceConfig = datasourceConfigs.get(DEFAULT_NAME);
		if (datasourceConfig == null) {// 默认数据源不存在则将第一个数据源作为默认数据源
			defaultName = firstName;
			datasourceConfig = datasourceConfigs.get(firstName);
		}
		defaultDataSource = DataSourceFactory.createDataSource(datasourceConfig);
		dataSources.put(defaultName, defaultDataSource);
		datasourceConfigs.remove(defaultName);
		for (Iterator<Entry<String, Properties>> it = datasourceConfigs.entrySet().iterator(); it.hasNext();) {
			Entry<String, Properties> entry = it.next();
			dataSources.put(entry.getKey(), DataSourceFactory.createDataSource(entry.getValue()));
		}
	}

	/**
	 * 获取默认数据源
	 * 
	 * @return 返回默认数据源
	 */
	public static DataSource getDefaultDataSource() {
		return defaultDataSource;
	}

	/**
	 * 根据数据源名称获取数据源
	 * 
	 * @param name
	 *            数据源名称
	 * @return 如果指定名称的数据源存在则返回该数据源，否则返回null
	 */
	public static DataSource getDataSource(String name) {
		return dataSources.get(name);
	}

}
