package cn.tenmg.sqltool.sql;

/**
 * 数据操纵语言转换器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public interface DMLParser {
	<T> DML parse(Class<T> type);
}