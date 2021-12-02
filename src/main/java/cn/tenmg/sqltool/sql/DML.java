package cn.tenmg.sqltool.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 数据操纵语言
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public class DML implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5158349537827613687L;

	private String sql;

	private List<Field> fields;

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

}
