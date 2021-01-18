package cn.tenmg.sqltool.config.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.filter.Blank;
import cn.tenmg.sqltool.config.model.filter.Eq;
import cn.tenmg.sqltool.config.model.filter.Gt;
import cn.tenmg.sqltool.config.model.filter.Gte;
import cn.tenmg.sqltool.config.model.filter.Lt;
import cn.tenmg.sqltool.config.model.filter.Lte;

/**
 * 参数过滤器配置模型
 * 
 * @author 赵伟均
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Filter {
	/**
	 * 空白字符串参数过滤器配置列表
	 */
	@XmlElement(name = "blank", namespace = Sqltool.NAMESPACE)
	private List<Blank> blanks;

	/**
	 * 等值参数过滤器配置列表
	 */
	@XmlElement(name = "eq", namespace = Sqltool.NAMESPACE)
	private List<Eq> Eqs;

	/**
	 * 大于给定值字符串参数移除器
	 */
	@XmlElement(name = "gt", namespace = Sqltool.NAMESPACE)
	private List<Gt> gts;

	/**
	 * 大于等于给定值字符串参数移除器
	 */
	@XmlElement(name = "gte", namespace = Sqltool.NAMESPACE)
	private List<Gte> gtes;

	/**
	 * 小于给定值字符串参数移除器
	 */
	@XmlElement(name = "lt", namespace = Sqltool.NAMESPACE)
	private List<Lt> lts;

	/**
	 * 小于等于给定值字符串参数处理器
	 */
	@XmlElement(name = "lte", namespace = Sqltool.NAMESPACE)
	private List<Lte> ltes;

	public List<Blank> getBlanks() {
		return blanks;
	}

	public void setBlanks(List<Blank> blanks) {
		this.blanks = blanks;
	}

	public List<Eq> getEqs() {
		return Eqs;
	}

	public void setEqs(List<Eq> eqs) {
		Eqs = eqs;
	}

	public List<Gt> getGts() {
		return gts;
	}

	public void setGts(List<Gt> gts) {
		this.gts = gts;
	}

	public List<Gte> getGtes() {
		return gtes;
	}

	public void setGtes(List<Gte> gtes) {
		this.gtes = gtes;
	}

	public List<Lt> getLts() {
		return lts;
	}

	public void setLts(List<Lt> lts) {
		this.lts = lts;
	}

	public List<Lte> getLtes() {
		return ltes;
	}

	public void setLtes(List<Lte> ltes) {
		this.ltes = ltes;
	}

}
