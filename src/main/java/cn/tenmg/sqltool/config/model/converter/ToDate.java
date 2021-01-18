package cn.tenmg.sqltool.config.model.converter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.Sqltool;

/**
 * 参数日期类型转换器配置模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class ToDate {
	/**
	 * 参数列表，使用逗号分隔
	 */
	@XmlAttribute
	private String params;

	/**
	 * 格式化模板（默认值yyyy-MM-dd）
	 */
	@XmlAttribute
	private String formatter = "yyyy-MM-dd";

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getFormatter() {
		return formatter;
	}

	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}

}
