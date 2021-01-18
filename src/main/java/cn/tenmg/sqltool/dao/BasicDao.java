package cn.tenmg.sqltool.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import cn.tenmg.sqltool.DSQLFactory;
import cn.tenmg.sqltool.datasource.DataSourceFactory;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.exception.InitializeDataSourceException;
import cn.tenmg.sqltool.factory.XMLFileDSQLFactory;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.SQLDialectUtils;

public class BasicDao extends AbstractDao {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5961378350698776883L;

	private static final String DATASOURCE_PREFIX = "sqltool.datasource.", DEFAULT_NAME = "default",
			DATASOURCE_REGEX = "^".concat(DATASOURCE_PREFIX.replaceAll("\\.", "\\\\."))
					.concat("([\\S]+\\.){0,1}[^\\.]+$");

	private static final int DATASOURCE_PREFIX_LEN = DATASOURCE_PREFIX.length();

	private static final Map<DataSource, SQLDialect> DIALECTS = new HashMap<DataSource, SQLDialect>();

	private static final Map<String, DataSource> dataSources = new HashMap<String, DataSource>();

	private static DataSource defaultDataSource;

	private BasicDao() {
		super();
	}

	private DSQLFactory dsqlFactory;

	private boolean showSql;

	private int defaultBatchSize = 500;

	public static BasicDao build(Properties properties) {
		String basePackages = properties.getProperty("sqltool.basePackages"),
				suffix = properties.getProperty("sqltool.suffix");
		BasicDao dao = new BasicDao();
		if (suffix == null) {
			dao.dsqlFactory = new XMLFileDSQLFactory(basePackages);
		} else {
			dao.dsqlFactory = new XMLFileDSQLFactory(basePackages, suffix);
		}
		dao.showSql = Boolean.valueOf(properties.getProperty("sqltool.showSql", "false"));
		dao.defaultBatchSize = Integer.valueOf(properties.getProperty("sqltool.defaultBatchSize", "500"));

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
		try {
			defaultDataSource = DataSourceFactory.createDataSource(datasourceConfig);
			dataSources.put(defaultName, defaultDataSource);
			DIALECTS.put(defaultDataSource, SQLDialectUtils.getSQLDialect(datasourceConfig));
			datasourceConfigs.remove(defaultName);
			DataSource dataSource;
			for (Iterator<Entry<String, Properties>> it = datasourceConfigs.entrySet().iterator(); it.hasNext();) {
				Entry<String, Properties> entry = it.next();
				datasourceConfig = entry.getValue();
				dataSource = DataSourceFactory.createDataSource(datasourceConfig);
				dataSources.put(entry.getKey(), dataSource);
				DIALECTS.put(dataSource, SQLDialectUtils.getSQLDialect(datasourceConfig));
			}
		} catch (Exception e) {
			throw new InitializeDataSourceException("An exception occurred while initializing datasource(s)", e);
		}
		return dao;
	}

	@Override
	public DSQLFactory getDSQLFactory() {
		return dsqlFactory;
	}

	@Override
	public DataSource getDefaultDataSource() {
		return defaultDataSource;
	}

	@Override
	public DataSource getDataSource(String name) {
		return dataSources.get(name);
	}

	@Override
	SQLDialect getSQLDialect(DataSource dataSource) {
		return DIALECTS.get(dataSource);
	}

	@Override
	boolean isShowSql() {
		return showSql;
	}

	@Override
	int getDefaultBatchSize() {
		return defaultBatchSize;
	}

}
