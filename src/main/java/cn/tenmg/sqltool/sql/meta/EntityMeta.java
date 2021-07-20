package cn.tenmg.sqltool.sql.meta;

import java.util.List;

/**
 * 实体类元数据
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
public class EntityMeta {

	private String tableName;

	private List<FieldMeta> fieldMetas;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<FieldMeta> getFieldMetas() {
		return fieldMetas;
	}

	public void setFieldMetas(List<FieldMeta> fieldMetas) {
		this.fieldMetas = fieldMetas;
	}

	public EntityMeta() {
		super();
	}

	public EntityMeta(String tableName, List<FieldMeta> fieldMetas) {
		super();
		this.tableName = tableName;
		this.fieldMetas = fieldMetas;
	}

}
