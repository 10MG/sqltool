package cn.tenmg.sqltool.sql;

/**
 * 数据操纵语言转换器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public interface DMLParser {
	<T> DML parse(Class<T> type);
}