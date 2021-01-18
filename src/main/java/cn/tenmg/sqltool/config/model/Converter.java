package cn.tenmg.sqltool.config.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.converter.ToDate;
import cn.tenmg.sqltool.config.model.converter.ToNumber;
import cn.tenmg.sqltool.config.model.converter.WrapString;

/**
 * 参数类型转换器配置模型
 * 
 * @author 赵伟均
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Converter {

	/**
	 * 参数日期类型转换器配置
	 */
	@XmlElement(name = "to-date", namespace = Sqltool.NAMESPACE)
	private List<ToDate> toDates;

	/**
	 * 参数数字类型转换器配置
	 */
	@XmlElement(name = "to-number", namespace = Sqltool.NAMESPACE)
	private List<ToNumber> toNumbers;

	/**
	 * 字符串LIKE参数包装转换器配置
	 */
	@XmlElement(name = "wrap-string", namespace = Sqltool.NAMESPACE)
	private List<WrapString> wrapStrings;

	public List<ToDate> getToDates() {
		return toDates;
	}

	public void setToDates(List<ToDate> toDates) {
		this.toDates = toDates;
	}

	public List<ToNumber> getToNumbers() {
		return toNumbers;
	}

	public void setToNumbers(List<ToNumber> toNumbers) {
		this.toNumbers = toNumbers;
	}

	public List<WrapString> getWrapStrings() {
		return wrapStrings;
	}

	public void setWrapStrings(List<WrapString> wrapStrings) {
		this.wrapStrings = wrapStrings;
	}

}
