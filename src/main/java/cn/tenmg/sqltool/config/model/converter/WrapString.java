package cn.tenmg.sqltool.config.model.converter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.Sqltool;

/**
 * 字符串参数包装转换器配置模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class WrapString {

	public static final String VALUE = "${value}";

	public static final String PARAM_REGEX = "\\$\\{value\\}";

	/**
	 * 参数列表，使用逗号分隔
	 */
	@XmlAttribute
	private String params;

	/**
	 * 包装模板（默认值%${value}%）
	 */
	@XmlAttribute
	private String formatter = "%".concat(VALUE).concat("%");

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
