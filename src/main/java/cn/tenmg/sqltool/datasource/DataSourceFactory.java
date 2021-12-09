package cn.tenmg.sqltool.datasource;

import java.util.Properties;

import javax.sql.DataSource;

import cn.tenmg.sqltool.exception.IllegalConfigException;

/**
 * 数据源工厂
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.2.0
 */
public final class DataSourceFactory {

	public static final String TYPE_NAME = "type", DEFAULT_TYPE = "com.alibaba.druid.pool.DruidDataSource",
			BUILDER_PREFIX = "cn.tenmg.sqltool.datasource.builder.", BUILDER_SUFFIX = "Builder";

	private DataSourceFactory() {
	}

	/**
	 * 创建数据源
	 * 
	 * @param properties
	 *            数据源配置
	 * @return 返回创建的数据源
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static DataSource createDataSource(Properties properties) throws Exception {
		String type = properties.getProperty(TYPE_NAME, DEFAULT_TYPE),
				buildName = BUILDER_PREFIX.concat(type).concat(BUILDER_SUFFIX);
		try {
			Class<DatasourceBuilder> datasourceBuilder = (Class<DatasourceBuilder>) Class.forName(buildName);
			return datasourceBuilder.newInstance().createDataSource(properties);
		} catch (ClassNotFoundException e) {
			throw new IllegalConfigException("This type of datasource is not supported at the moment: " + type, e);
		}
	}

}
