package cn.tenmg.sqltool.factory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import cn.tenmg.sqltool.SqltoolFactory;
import cn.tenmg.sqltool.config.ConfigLoader;
import cn.tenmg.sqltool.config.loader.XMLConfigLoader;
import cn.tenmg.sqltool.config.model.Dsql;
import cn.tenmg.sqltool.config.model.Sqltool;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.utils.ClassUtils;
import cn.tenmg.sqltool.utils.CollectionUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 基于XML文件配置的Sqltool工厂
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class XMLFileSqltoolFactory extends AbstractSqltoolFactory implements SqltoolFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8125151681490092061L;

	private static final Logger log = Logger.getLogger(XMLFileSqltoolFactory.class);

	private static final ConfigLoader loader = XMLConfigLoader.getInstance();

	private String basePackages;

	private String suffix = ".dsql.xml";

	public String getBasePackages() {
		return basePackages;
	}

	public void setBasePackages(String basePackages) {
		this.basePackages = basePackages;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	private XMLFileSqltoolFactory() {
		super();
	}

	public static final XMLFileSqltoolFactory bind(String basePackages) {
		XMLFileSqltoolFactory factor = new XMLFileSqltoolFactory();
		factor.setBasePackages(basePackages);
		factor.init();
		return factor;
	}

	public static final XMLFileSqltoolFactory bind(String basePackages, String suffix) {
		XMLFileSqltoolFactory factor = new XMLFileSqltoolFactory();
		factor.setBasePackages(basePackages);
		factor.setSuffix(suffix);
		factor.init();
		return factor;
	}

	private void init() {
		if (StringUtils.isBlank(basePackages)) {
			log.warn("参数basePackages没有配置");
		} else {
			String[] basePackages = this.basePackages.split(",");
			for (int i = 0; i < basePackages.length; i++) {
				String basePackage = basePackages[i];
				try {
					if (log.isInfoEnabled()) {
						log.info("扫描包：".concat(basePackage));
					}
					List<Object> files = getDsqlFiles(basePackage.replaceAll("\\.", "/"));
					if (CollectionUtils.isEmpty(files)) {
						log.warn("包：".concat(basePackage).concat("没有找到后缀名为").concat(suffix).concat("的文件"));
					} else {
						for (int j = 0, size = files.size(); j < size; j++) {
							Object file = files.get(j);
							Sqltool sqltool;
							String fileName;
							if (file instanceof File) {
								File f = (File) file;
								fileName = f.getName();
								if (log.isInfoEnabled()) {
									log.info("开始解析".concat(fileName));
								}
								sqltool = loader.load(f);
							} else {
								fileName = (String) file;
								if (log.isInfoEnabled()) {
									log.info("开始解析".concat(fileName));
								}
								ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
								InputStream is = classLoader.getResourceAsStream(fileName);
								sqltool = loader.load(is);
							}
							List<Dsql> dsqls = sqltool.getDsql();
							if (!CollectionUtils.isEmpty(dsqls)) {
								for (Iterator<Dsql> dit = dsqls.iterator(); dit.hasNext();) {
									Dsql dsql = dit.next();
									this.dsqls.put(dsql.getId(), dsql);
								}
							}
							if (log.isInfoEnabled()) {
								log.info("完成解析".concat(fileName));
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					String msg = "扫描包：".concat(basePackage).concat("加载SQL文件失败");
					throw new IllegalConfigException(msg, e);
				}
			}
		}
	}

	/**
	 * 获取指定目录下的所有DSQL配置文件
	 * 
	 * @param dir
	 *            指定目录
	 * @return
	 * @throws IOException
	 *             发生I/O异常
	 * @throws URISyntaxException
	 *             统一资源标识符语法错误异常
	 */
	private List<Object> getDsqlFiles(String dir) throws IOException, URISyntaxException {
		dir = dir.trim();
		List<Object> result = new ArrayList<Object>();
		Enumeration<URL> urls = ClassUtils.getDefaultClassLoader().getResources(dir);
		if (urls != null) {
			URL url;
			while (urls.hasMoreElements()) {
				url = urls.nextElement();
				if (url.getProtocol().equals("jar")) {
					JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
					Enumeration<JarEntry> entries = jar.entries();
					JarEntry entry;
					String name;
					while (entries.hasMoreElements()) {// 获取jar里的一个实体 可以是目录也可以是文件
						entry = entries.nextElement();
						name = entry.getName();
						if (!entry.isDirectory() && name.endsWith(suffix)) {
							result.add(name);
						}
					}
				} else {
					getDsqlFiles(new File(url.toURI()), result);
				}
			}
		}
		return result;
	}

	/**
	 * 获取目录下的所有DSQL配置文件
	 * 
	 * @param parent
	 *            目录
	 * @param files
	 *            结果列表
	 */
	private void getDsqlFiles(File parent, List<Object> files) {
		String fileName = parent.getName();
		if (parent.isDirectory()) {
			File file, listFiles[] = parent.listFiles();
			for (int i = 0; i < listFiles.length; i++) {
				file = listFiles[i];
				fileName = file.getName();
				if (file.isDirectory()) {
					getDsqlFiles(listFiles[i], files);
				} else {
					if (fileName.endsWith(suffix)) {
						files.add(file);
					}
				}
			}
		} else if (fileName.endsWith(suffix)) {
			files.add(parent);
		}
	}

}
