package cn.tenmg.sqltool.sql.meta;

import java.lang.reflect.Field;

/**
 * 属性元数据
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
public class FieldMeta {

	private Field field;

	private String columnName;

	private boolean id;

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public boolean isId() {
		return id;
	}

	public void setId(boolean id) {
		this.id = id;
	}

	public FieldMeta(Field field, String columnName) {
		super();
		this.field = field;
		this.columnName = columnName;
	}

}
