package cn.tenmg.sqltool.sql;

import java.io.Serializable;
import java.util.List;

import cn.tenmg.sqltool.sql.meta.FieldMeta;

/**
 * 合并数据操作对象
 * 
 * @author 赵伟均
 *
 */
public class MergeSql implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8160135968550136566L;

	/**
	 * SQL
	 */
	private String script;

	/**
	 * 字段列表
	 */
	private List<FieldMeta> fieldMetas;

	public MergeSql() {
		super();
	}

	public MergeSql(String script, List<FieldMeta> fieldMetas) {
		super();
		this.script = script;
		this.fieldMetas = fieldMetas;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public List<FieldMeta> getFieldMetas() {
		return fieldMetas;
	}

	public void setFieldMetas(List<FieldMeta> fieldMetas) {
		this.fieldMetas = fieldMetas;
	}

}
