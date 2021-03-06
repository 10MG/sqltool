package cn.tenmg.sqltool.datasource;

import java.util.Properties;

import javax.sql.DataSource;

/**
 * 数据源构建器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface DatasourceBuilder {

	DataSource createDataSource(Properties properties) throws Exception;

}
