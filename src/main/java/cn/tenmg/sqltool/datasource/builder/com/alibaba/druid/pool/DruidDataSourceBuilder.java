package cn.tenmg.sqltool.datasource.builder.com.alibaba.druid.pool;

import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import cn.tenmg.sqltool.datasource.DatasourceBuilder;

/**
 * druid数据源构建器
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.2.0
 */
public class DruidDataSourceBuilder implements DatasourceBuilder {

	@Override
	public DataSource createDataSource(Properties properties) throws Exception {
		return DruidDataSourceFactory.createDataSource(properties);
	}

}
