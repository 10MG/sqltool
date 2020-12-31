package cn.tenmg.sqltool.config.loader;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cn.tenmg.sqltool.config.ConfigLoader;
import cn.tenmg.sqltool.config.model.Sqltool;
import cn.tenmg.sqltool.exception.IllegalConfigException;

/**
 * XML配置加载器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class XMLConfigLoader implements ConfigLoader {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3321576666700440538L;

	private static class InstanceHolder {
		private static final XMLConfigLoader INSTANCE = new XMLConfigLoader();
	}

	public static final XMLConfigLoader getInstance() {
		return InstanceHolder.INSTANCE;
	}

	/**
	 * 加载sqltool配置
	 * 
	 * @param s
	 *            配置字符串
	 * @return sqltool配置模型
	 */
	public Sqltool load(String s) {
		try {
			JAXBContext context = JAXBContext.newInstance(Sqltool.class);
			Unmarshaller us = context.createUnmarshaller();
			return (Sqltool) us.unmarshal(new StringReader(s));
		} catch (JAXBException e) {
			e.printStackTrace();
			final String msg = "加载sqltool配置失败";
			throw new IllegalConfigException(msg, e);
		}
	}

	/**
	 * 加载sqltool配置
	 * 
	 * @param file
	 *            配置文件
	 * @return sqltool配置模型
	 */
	public Sqltool load(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(Sqltool.class);
			Unmarshaller us = context.createUnmarshaller();
			return (Sqltool) us.unmarshal(file);
		} catch (JAXBException e) {
			e.printStackTrace();
			final String msg = "加载sqltool配置失败";
			throw new IllegalConfigException(msg, e);
		}
	}

	/**
	 * 加载sqltool配置
	 * 
	 * @param fr
	 *            文件读取器
	 * @return sqltool配置模型
	 */
	public Sqltool load(FileReader fr) {
		try {
			JAXBContext context = JAXBContext.newInstance(Sqltool.class);
			Unmarshaller us = context.createUnmarshaller();
			return (Sqltool) us.unmarshal(fr);
		} catch (JAXBException e) {
			e.printStackTrace();
			final String msg = "加载sqltool配置失败";
			throw new IllegalConfigException(msg, e);
		}
	}

	/**
	 * 加载sqltool配置
	 * 
	 * @param is
	 *            输入流
	 * @return sqltool配置模型
	 */
	@Override
	public Sqltool load(InputStream is) {
		try {
			JAXBContext context = JAXBContext.newInstance(Sqltool.class);
			Unmarshaller us = context.createUnmarshaller();
			return (Sqltool) us.unmarshal(is);
		} catch (JAXBException e) {
			e.printStackTrace();
			final String msg = "加载sqltool配置失败";
			throw new IllegalConfigException(msg, e);
		}
	}
}
