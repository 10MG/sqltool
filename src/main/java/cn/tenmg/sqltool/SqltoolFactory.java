package cn.tenmg.sqltool;

import java.lang.reflect.Method;
import java.util.Properties;

import cn.tenmg.sqltool.dao.BasicDao;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.utils.PropertiesLoaderUtils;

/**
 * Sqltool工厂
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class SqltoolFactory {

	public static final String DAO_CONFIG_KEY = "sqltool.dao", DEFAULT_DAO = "cn.tenmg.sqltool.dao.BasicDao";

	/**
	 * 创建数据库访问对象
	 * 
	 * @param properties
	 *            配置属性
	 * @return 返回数据库访问对象
	 */
	public static Dao createDao(Properties properties) {
		return BasicDao.build(properties);
	}

	/**
	 * 创建数据库访问对象
	 * 
	 * @param pathInClassPath
	 *            配置文件相对于classPath的路径
	 * 
	 * @return 返回数据库访问对象
	 */
	public static Dao createDao(String pathInClassPath) {
		try {
			Properties properties = PropertiesLoaderUtils.loadFromClassPath(pathInClassPath);
			String dao = properties.getProperty(DAO_CONFIG_KEY);
			if (dao == null) {
				dao = DEFAULT_DAO;
			}
			Class<?> cls = Class.forName(dao);
			Method method = cls.getMethod("build", Properties.class);
			return (Dao) method.invoke(null, properties);
		} catch (Exception e) {
			throw new IllegalConfigException("Exception occurred when building database access object", e);
		}
	}

}
