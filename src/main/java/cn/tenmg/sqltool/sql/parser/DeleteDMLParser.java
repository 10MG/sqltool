package cn.tenmg.sqltool.sql.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 删除数据数据库操纵语言解析器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class DeleteDMLParser extends AbstractDMLParser {

	private static final String DELETE = "DELETE FROM %s WHERE %s";

	private static class InstanceHolder {
		private static final DeleteDMLParser INSTANCE = new DeleteDMLParser();
	}

	public static final DeleteDMLParser getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	protected <T> void parseDML(DML dml, Class<T> type, String tableName) {
		StringBuilder criteria = new StringBuilder();
		List<Field> idFields = new ArrayList<Field>();
		boolean criteriaFlag = false;
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		if (entityMeta == null) {
			Class<?> current = type;
			Set<String> fieldSet = new HashSet<String>();
			List<Field> fields = new ArrayList<Field>();
			List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
			while (!Object.class.equals(current)) {
				Field field, declaredFields[] = current.getDeclaredFields();
				for (int i = 0; i < declaredFields.length; i++) {
					field = declaredFields[i];
					String fieldName = field.getName();
					if (!fieldSet.contains(fieldName)) {
						fieldSet.add(fieldName);
						Column column = field.getAnnotation(Column.class);
						if (column != null) {
							field.setAccessible(true);
							fields.add(field);
							String columnName = column.name();
							if (StringUtils.isBlank(columnName)) {
								columnName = StringUtils.camelToUnderline(fieldName, true);
							}
							FieldMeta fieldMeta = new FieldMeta(field, columnName);
							if (field.getAnnotation(Id.class) == null) {
								fieldMeta.setId(false);
							} else {
								fieldMeta.setId(true);
								idFields.add(field);
								if (criteriaFlag) {
									criteria.append(JdbcUtils.SPACE_AND_SPACE);
								} else {
									criteriaFlag = true;
								}
								criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE)
										.append(JdbcUtils.PARAM_MARK);
							}
							fieldMetas.add(fieldMeta);
						}
					}
				}
				current = current.getSuperclass();
			}
			EntityUtils.cacheEntityMeta(type, new EntityMeta(EntityUtils.getTableName(type), fieldMetas));
		} else {
			List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
			FieldMeta fieldMeta;
			for (int i = 0, size = fieldMetas.size(); i < size; i++) {
				fieldMeta = fieldMetas.get(i);
				String columnName = fieldMeta.getColumnName();
				if (fieldMeta.isId()) {
					idFields.add(fieldMeta.getField());
					if (criteriaFlag) {
						criteria.append(JdbcUtils.SPACE_AND_SPACE);
					} else {
						criteriaFlag = true;
					}
					criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.PARAM_MARK);
				}
			}
		}
		if (criteriaFlag) {
			dml.setSql(String.format(DELETE, tableName, criteria));
			dml.setFields(idFields);
		} else {
			throw new PkNotFoundException(
					"Primary key not found in class ".concat(type.getName()).concat(", please use @Id to config"));
		}
	}

}
