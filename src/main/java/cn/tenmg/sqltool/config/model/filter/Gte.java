package cn.tenmg.sqltool.config.model.filter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import cn.tenmg.sqltool.config.model.Sqltool;

/**
 * 大于等于给定值字符串参数移除器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
@XmlRootElement(namespace = Sqltool.NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class Gte extends Eq {

}
