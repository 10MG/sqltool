package cn.tenmg.sqltool.sql;

import java.io.Serializable;

import cn.tenmg.sqltool.dsql.NamedSQL;

/**
 * SQL引擎。用于解析带参数的SQL为可执行SQL
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface SQLEngine extends Serializable {

	/**
	 * 将动态SQL解析后的对象转换成可执行SQL
	 * 
	 * @param namedSQL
	 *            使用命名参数的SQL对象
	 * @return 返回转换后的可执行SQL
	 */
	String parse(NamedSQL namedSQL);

}
