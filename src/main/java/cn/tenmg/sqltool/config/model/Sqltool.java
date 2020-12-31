package cn.tenmg.sqltool.config.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * sqltool配置模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Sqltool implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 927678194402355324L;

	/**
	 * 可扩展标记语言（XML）模式定义（Schemas Definition）文件的命名空间
	 */
	public static final String NAMESPACE = "http://www.10mg.cn/schema/sqltool";

	/**
	 * 动态SQL配置列表
	 */
	@XmlElement(namespace = NAMESPACE)
	private List<Dsql> dsql;

	public List<Dsql> getDsql() {
		return dsql;
	}

	public void setDsql(List<Dsql> dsql) {
		this.dsql = dsql;
	}

}
