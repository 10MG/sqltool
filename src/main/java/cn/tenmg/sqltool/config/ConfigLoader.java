package cn.tenmg.sqltool.config;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Serializable;

import cn.tenmg.sqltool.config.model.Sqltool;

/**
 * 配置加载器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface ConfigLoader extends Serializable {

	/**
	 * 加载sqltool配置
	 * 
	 * @param s
	 *            配置字符串
	 * @return sqltool配置模型
	 */
	Sqltool load(String s);

	/**
	 * 加载sqltool配置
	 * 
	 * @param file
	 *            配置文件
	 * @return sqltool配置模型
	 */
	Sqltool load(File file);

	/**
	 * 加载sqltool配置
	 * 
	 * @param fr
	 *            文件读取器
	 * @return sqltool配置模型
	 */
	Sqltool load(FileReader fr);

	/**
	 * 加载sqltool配置
	 * 
	 * @param is
	 *            输入流
	 * @return sqltool配置模型
	 */
	Sqltool load(InputStream is);
}
