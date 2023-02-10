package cn.tenmg.sqltool.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import cn.tenmg.dsl.utils.MapUtils;
import cn.tenmg.dsql.DSQLFactory;
import cn.tenmg.dsql.factory.XMLFileDSQLFactory;
import cn.tenmg.sqltool.datasource.DataSourceFactory;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.InitializeDataSourceException;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

/**
 * 基本数据库访问对象
 * 
 * @author June wjzhao@aliyun.com
 *
 */
public class BasicDao extends AbstractDao {

	private static final String DATASOURCE_PREFIX = "sqltool.datasource.", DEFAULT_NAME = "default",
			DATASOURCE_REGEX = "^".concat(DATASOURCE_PREFIX.replaceAll("\\.", "\\\\."))
					.concat("([\\S]+\\.){0,1}[^\\.]+$");

	private static final int DATASOURCE_PREFIX_LEN = DATASOURCE_PREFIX.length();

	private DataSource defaultDataSource;

	private Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

	private DSQLFactory DSQLFactory;

	private boolean showSql;

	private int defaultBatchSize = 500;

	public void setDefaultDataSource(DataSource defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	@Override
	public DataSource getDefaultDataSource() {
		return defaultDataSource;
	}

	@Override
	public DataSource getDataSource(String name) {
		return dataSources.get(name);
	}

	public Map<String, DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	public void setDSQLFactory(DSQLFactory dSQLFactory) {
		DSQLFactory = dSQLFactory;
	}

	@Override
	public DSQLFactory getDSQLFactory() {
		return DSQLFactory;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	@Override
	public boolean isShowSql() {
		return showSql;
	}

	public void setDefaultBatchSize(int defaultBatchSize) {
		this.defaultBatchSize = defaultBatchSize;
	}

	@Override
	public int getDefaultBatchSize() {
		return defaultBatchSize;
	}

	public BasicDao() {
		super();
	}

	public static BasicDao build(Properties properties) {
		return new BasicDao(properties);
	}

	private BasicDao(Properties properties) {
		super();
		String basePackages = properties.getProperty("sqltool.basePackages"),
				suffix = properties.getProperty("sqltool.suffix");
		if (suffix == null) {
			this.DSQLFactory = new XMLFileDSQLFactory(basePackages);
		} else {
			this.DSQLFactory = new XMLFileDSQLFactory(basePackages, suffix);
		}
		this.showSql = Boolean.valueOf(properties.getProperty("sqltool.showSql", "false"));
		this.defaultBatchSize = Integer.valueOf(properties.getProperty("sqltool.defaultBatchSize", "500"));
		Map<String, Properties> dataSourceConfigs = new HashMap<String, Properties>();
		String key, name, param, firstName = null;
		Object value;
		Properties dataSourceConfig;
		for (Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();
			key = entry.getKey().toString();
			value = entry.getValue();
			if (key.matches(DATASOURCE_REGEX)) {
				param = key.substring(DATASOURCE_PREFIX_LEN);
				int index = param.indexOf(".");
				if (index > 0) {
					name = param.substring(0, index);
					param = param.substring(index + 1);
				} else {
					name = DEFAULT_NAME;
				}
				if (firstName == null) {
					firstName = name;
				}
				dataSourceConfig = dataSourceConfigs.get(name);
				if (dataSourceConfig == null) {
					dataSourceConfig = new Properties();
					dataSourceConfigs.put(name, dataSourceConfig);
				}
				dataSourceConfig.put(param, value);
			}
		}
		if (MapUtils.isEmpty(dataSourceConfigs)) {
			throw new IllegalConfigException("No datasource is configured, please check the configuration");
		}
		String defaultName = DEFAULT_NAME;
		dataSourceConfig = dataSourceConfigs.get(DEFAULT_NAME);
		if (dataSourceConfig == null) {// 默认数据源不存在则将第一个数据源作为默认数据源
			defaultName = firstName;
			dataSourceConfig = dataSourceConfigs.get(firstName);
		}
		try {
			defaultDataSource = DataSourceFactory.createDataSource(dataSourceConfig);
			dataSources.put(defaultName, defaultDataSource);
			cacheSQLDialect(defaultDataSource, SQLDialectUtils.getSQLDialect(dataSourceConfig));
			dataSourceConfigs.remove(defaultName);
			DataSource dataSource;
			for (Iterator<Entry<String, Properties>> it = dataSourceConfigs.entrySet().iterator(); it.hasNext();) {
				Entry<String, Properties> entry = it.next();
				dataSourceConfig = entry.getValue();
				dataSource = DataSourceFactory.createDataSource(dataSourceConfig);
				dataSources.put(entry.getKey(), dataSource);
				cacheSQLDialect(dataSource, SQLDialectUtils.getSQLDialect(dataSourceConfig));
			}
		} catch (Exception e) {
			throw new InitializeDataSourceException("An exception occurred while initializing datasource(s)", e);
		}
		closeDataSourcesWhenShutdown(dataSources.values());
	}
}
