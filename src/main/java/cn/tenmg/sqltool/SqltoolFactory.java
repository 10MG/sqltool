package cn.tenmg.sqltool;

import java.io.Serializable;
import java.util.Map;

import cn.tenmg.sqltool.dsql.NamedSQL;

/**
 * Sqltool工厂
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface SqltoolFactory extends Serializable {

	/**
	 * 根据指定编号获取SQL/动态SQL(DSQL)脚本
	 * 
	 * @param id
	 *            指定编号
	 * @return SQL脚本
	 */
	String getScript(String id);

	/**
	 * 根据指定的参数params分析转换动态SQL（dsql）为SQL。dsql可以是工厂中动态SQL的编号(id)，也可以是动态SQL脚本
	 * 
	 * @param dsql
	 *            动态SQL的编号(id)或者动态SQL脚本
	 * @param params
	 *            参数列表(分别列出参数名和参数值，或使用一个Map对象)
	 * @return SQL对象
	 */
	NamedSQL parse(String dsql, Object... params);

	/**
	 * 根据指定的参数params分析转换动态SQL（dsql）为SQL。dsql可以是工厂中动态SQL的编号(id)，也可以是动态SQL脚本
	 * 
	 * @param dsql
	 *            动态SQL的编号(id)或者动态SQL脚本
	 * @param params
	 *            参数列表
	 * @return SQL对象
	 */
	NamedSQL parse(String dsql, Map<String, ?> params);

}
