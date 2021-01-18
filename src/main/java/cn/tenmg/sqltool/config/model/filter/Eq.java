package cn.tenmg.sqltool.config.model.filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.Sqltool;

/**
 * 等值参数过滤器配置模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Eq {
	/**
	 * 参数列表，使用逗号分隔。默认*，代表全部
	 */
	@XmlAttribute
	private String params = "*";

	/**
	 * 供比较的值
	 */
	@XmlAttribute
	private String value;

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
