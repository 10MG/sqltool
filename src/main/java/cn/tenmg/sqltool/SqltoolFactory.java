package cn.tenmg.sqltool;

import java.io.IOException;
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
			return BasicDao.build(PropertiesLoaderUtils.loadFromClassPath(pathInClassPath));
		} catch (IOException e) {
			throw new IllegalConfigException("Exception occurred when loading configuration", e);
		}
	}

}
