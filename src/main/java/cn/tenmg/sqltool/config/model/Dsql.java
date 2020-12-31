package cn.tenmg.sqltool.config.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 动态SQL配置模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Dsql implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6101977085012651699L;

	/**
	 * SQL编号
	 */
	@XmlAttribute
	private String id;

	/**
	 * 动态SQL配置
	 */
	@XmlElement(namespace = Sqltool.NAMESPACE)
	private String script;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

}
