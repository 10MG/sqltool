package cn.tenmg.sqltool.sql;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 更新数据操作对象
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.1.1
 */
public class UpdateSQL implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3643319049849761812L;

	/**
	 * SQL
	 */
	private String script;

	/**
	 * 字段列表
	 */
	private List<Field> fields;

	public UpdateSQL() {
		super();
	}

	public UpdateSQL(String script, List<Field> fields) {
		super();
		this.script = script;
		this.fields = fields;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

}
