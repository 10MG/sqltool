package cn.tenmg.sqltool.sql;

import java.io.Serializable;

/**
 * 数据操纵语言转换器
 * 
 * @author 赵伟均
 *
 */
public interface DMLParser extends Serializable {
	<T> DML parse(Class<T> type);
}