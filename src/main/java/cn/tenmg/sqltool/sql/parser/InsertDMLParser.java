package cn.tenmg.sqltool.sql.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.ColumnNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 插入数据操纵语言解析器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class InsertDMLParser extends AbstractDMLParser {

	private static final String INSERT = "INSERT INTO %s(%s) VALUES (%s)";

	private static class InstanceHolder {
		private static final InsertDMLParser INSTANCE = new InsertDMLParser();
	}

	public static final InsertDMLParser getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	protected <T> void parseDML(DML dml, Class<T> type, String tableName) {
		boolean flag = false;
		List<Field> fields = new ArrayList<Field>();
		StringBuilder columns = new StringBuilder(), values = new StringBuilder();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		if (entityMeta == null) {
			Class<?> current = type;
			Set<String> fieldSet = new HashSet<String>();
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
							}
							if (flag) {
								columns.append(JdbcUtils.COMMA_SPACE);
								values.append(JdbcUtils.COMMA_SPACE);
							} else {
								flag = true;
							}
							columns.append(columnName);
							values.append(JdbcUtils.PARAM_MARK);
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
				fields.add(fieldMeta.getField());
				if (flag) {
					columns.append(JdbcUtils.COMMA_SPACE);
					values.append(JdbcUtils.COMMA_SPACE);
				} else {
					flag = true;
				}
				columns.append(fieldMeta.getColumnName());
				values.append(JdbcUtils.PARAM_MARK);
			}
		}
		if (flag) {
			dml.setSql(String.format(INSERT, tableName, columns, values));
			dml.setFields(fields);
		} else {
			throw new ColumnNotFoundException(
					"Column not found in class ".concat(type.getName()).concat(", please use @Column to config"));
		}
	}

}
