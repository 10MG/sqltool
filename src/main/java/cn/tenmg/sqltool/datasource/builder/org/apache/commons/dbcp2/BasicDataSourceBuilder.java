package cn.tenmg.sqltool.datasource.builder.org.apache.commons.dbcp2;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSourceFactory;

import cn.tenmg.sqltool.datasource.DatasourceBuilder;

public class BasicDataSourceBuilder implements DatasourceBuilder {

	@Override
	public DataSource createDataSource(Properties properties) throws Exception {
		return BasicDataSourceFactory.createDataSource(properties);
	}

}
