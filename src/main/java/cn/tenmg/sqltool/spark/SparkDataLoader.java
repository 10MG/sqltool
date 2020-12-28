package cn.tenmg.sqltool.spark;

import java.io.Serializable;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import cn.tenmg.sqltool.SqltoolFactory;
import cn.tenmg.sqltool.dsql.NamedSQL;
import cn.tenmg.sqltool.sql.engine.SparkSQLEngine;
import cn.tenmg.sqltool.utils.SQLEngineUtils;

/**
 * Spark数据加载器
 * 
 * @author 赵伟均
 *
 */
public class SparkDataLoader implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3061843065600681110L;

	private SqltoolFactory sqltoolFactory;

	public SparkDataLoader(SqltoolFactory sqltoolFactory) {
		super();
		this.sqltoolFactory = sqltoolFactory;
	}

	/**
	 * 从数据库加载数据集
	 * 
	 * @param sparkSession
	 *            spark会话
	 * @param options
	 *            数据库配置项
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数集
	 * @return 返回加载的数据集
	 */
	public Dataset<Row> load(SparkSession sparkSession, Map<String, String> options, String dsql, Object... params) {
		return load(sparkSession, options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 从数据库加载数据集
	 * 
	 * @param sparkSession
	 *            Spark会话
	 * @param options
	 *            数据库配置项
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数集
	 * @return 返回加载的数据集
	 */
	public Dataset<Row> load(SparkSession sparkSession, Map<String, String> options, String dsql,
			Map<String, Object> params) {
		return load(sparkSession, options, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 执行SparkSQL查询
	 * 
	 * @param sparkSession
	 *            spark会话
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数集
	 * @return 返回查询的数据集
	 */
	public Dataset<Row> sql(SparkSession sparkSession, String dsql, Object... params) {
		return sql(sparkSession, sqltoolFactory.parse(dsql, params));
	}

	/**
	 * 执行SparkSQL查询
	 * 
	 * @param sparkSession
	 *            spark会话
	 * @param dsql
	 *            动态结构化查询语言
	 * @param params
	 *            查询参数集
	 * @return 返回查询的数据集
	 */
	public Dataset<Row> sql(SparkSession sparkSession, String dsql, Map<String, Object> params) {
		return sql(sparkSession, sqltoolFactory.parse(dsql, params));
	}

	private Dataset<Row> load(SparkSession sparkSession, Map<String, String> options, NamedSQL namedSQL) {
		return sparkSession.sqlContext().read().options(options)
				.option("query", SQLEngineUtils.getSqlEngine(options.get("url")).parse(namedSQL)).format("jdbc").load();
	}

	private Dataset<Row> sql(SparkSession sparkSession, NamedSQL namedSQL) {
		return sparkSession.sqlContext().sql(SparkSQLEngine.getInstance().parse(namedSQL));
	}
}