package cn.tenmg.sqltool.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sqltool.SqltoolContext;
import cn.tenmg.sqltool.sql.SQLDialect;

/**
 * 方言工具类
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.1.0
 */
public class SQLDialectUtils {

	private static final String JDBC_PRODUCT_SPLIT = ":", SQL_DIALECT_PREFIX = "sql.dialect.",
			GET_INSTANCE_METHOD = "getInstance";

	private static volatile Map<String, SQLDialect> URL_KEY_DIALECTS = new HashMap<String, SQLDialect>(),
			PRODUCT_KEY_DIALECTS = new HashMap<String, SQLDialect>();

	public static SQLDialect getSQLDialect(Map<String, String> options) {
		return getSQLDialect(options.get("url"));
	}

	public static SQLDialect getSQLDialect(Properties properties) {
		return getSQLDialect(properties.getProperty("url"));
	}

	@SuppressWarnings("unchecked")
	public static SQLDialect getSQLDialect(String url) {
		SQLDialect dialect = null;
		if (URL_KEY_DIALECTS.containsKey(url)) {
			dialect = URL_KEY_DIALECTS.get(url);
		} else if (url != null) {
			synchronized (URL_KEY_DIALECTS) {
				if (URL_KEY_DIALECTS.containsKey(url)) {
					dialect = URL_KEY_DIALECTS.get(url);
				} else {
					String tmp = url.substring(url.indexOf(JDBC_PRODUCT_SPLIT) + 1),
							product = tmp.substring(0, tmp.indexOf(JDBC_PRODUCT_SPLIT));
					if (PRODUCT_KEY_DIALECTS.containsKey(product)) {
						dialect = PRODUCT_KEY_DIALECTS.get(product);
					} else {
						synchronized (PRODUCT_KEY_DIALECTS) {
							if (PRODUCT_KEY_DIALECTS.containsKey(product)) {
								dialect = PRODUCT_KEY_DIALECTS.get(product);
							} else {
								String className = SqltoolContext.getProperty(
										SQL_DIALECT_PREFIX + tmp.substring(0, tmp.indexOf(JDBC_PRODUCT_SPLIT)));
								try {
									Class<SQLPagingDialect> cls = (Class<SQLPagingDialect>) Class.forName(className);
									Method method;
									try {
										if ((method = cls.getMethod(GET_INSTANCE_METHOD)) != null) {
											try {
												dialect = (SQLDialect) method.invoke(null);
											} catch (IllegalAccessException | IllegalArgumentException
													| InvocationTargetException e) {
												e.printStackTrace();
											}
										}
									} catch (NoSuchMethodException | SecurityException e) {
										try {
											dialect = (SQLDialect) cls.newInstance();
										} catch (InstantiationException | IllegalAccessException ex) {
											ex.printStackTrace();
										}
									}
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
								PRODUCT_KEY_DIALECTS.put(product, dialect);
							}
						}
					}
					PRODUCT_KEY_DIALECTS.put(url, dialect);
				}
			}
		}
		return dialect;
	}

}
