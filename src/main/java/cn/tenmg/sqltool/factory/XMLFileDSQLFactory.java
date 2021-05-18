package cn.tenmg.sqltool.factory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.tenmg.sqltool.config.ConfigLoader;
import cn.tenmg.sqltool.config.loader.XMLConfigLoader;
import cn.tenmg.sqltool.config.model.Dsql;
import cn.tenmg.sqltool.config.model.Sqltool;
import cn.tenmg.sqltool.exception.IllegalConfigException;
import cn.tenmg.sqltool.utils.ClassUtils;
import cn.tenmg.sqltool.utils.CollectionUtils;

/**
 * 基于XML文件配置的动态结构化查询语言工厂
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class XMLFileDSQLFactory extends AbstractDSQLFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8125151681490092061L;

	private static final Logger log = LogManager.getLogger(XMLFileDSQLFactory.class);

	private static final ConfigLoader loader = XMLConfigLoader.getInstance();

	private final Map<String, Dsql> dsqls = new HashMap<String, Dsql>();

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

	public XMLFileDSQLFactory(String basePackages) {
		this.basePackages = basePackages;
		init();
	}

	public XMLFileDSQLFactory(String basePackages, String suffix) {
		this.basePackages = basePackages;
		this.suffix = suffix;
		init();
	}

	@Override
	Map<String, Dsql> getDsqls() {
		return dsqls;
	}

	private void init() {
		if (basePackages == null) {
			log.warn("The parameter basePackages is null");
		} else {
			String[] basePackages = this.basePackages.split(",");
			for (int i = 0; i < basePackages.length; i++) {
				String basePackage = basePackages[i],
						basePath = basePackage.replaceAll("\\.", Matcher.quoteReplacement(File.separator)),
						fileName = null, fullName;
				try {
					if (log.isInfoEnabled()) {
						log.info("Scan package: ".concat(basePackage));
					}
					List<Object> files = getDsqlFiles(basePath);
					if (CollectionUtils.isEmpty(files)) {
						log.warn("The ".concat(suffix).concat(" file was not found in package : ").concat(basePackage));
					} else {
						for (int j = 0, size = files.size(); j < size; j++) {
							Object file = files.get(j);
							Sqltool sqltool;
							if (file instanceof File) {
								File f = (File) file;
								fileName = basePath.concat(File.separator).concat(f.getName());
								log.info("Starting parse: ".concat(fileName));
								sqltool = loader.load(f);
							} else {
								fullName = (String) file;
								fileName = fullName.substring(basePath.lastIndexOf(basePath));
								log.info("Starting parse: ".concat(fileName));
								ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
								InputStream is = classLoader.getResourceAsStream(fullName);
								sqltool = loader.load(is);
							}
							List<Dsql> dsqls = sqltool.getDsql();
							if (!CollectionUtils.isEmpty(dsqls)) {
								for (Iterator<Dsql> dit = dsqls.iterator(); dit.hasNext();) {
									Dsql dsql = dit.next();
									this.dsqls.put(dsql.getId(), dsql);
								}
							}
							log.info("Finished parse: ".concat(fileName));
						}
					}
				} catch (Exception e) {
					String msg;
					if (fileName == null) {
						msg = "Failed to scan package: " + basePackage;
					} else {
						msg = "Failed to load file: " + fileName + " in package: " + basePackage;
					}
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
