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
 * 更新数据操纵语言解析器
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class UpdateDMLParser extends AbstractDMLParser {

	private static final String UPDATE = "UPDATE %s SET %s WHERE %s";

	private static class InstanceHolder {
		private static final UpdateDMLParser INSTANCE = new UpdateDMLParser();
	}

	public static final UpdateDMLParser getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	protected <T> void parseDML(DML dml, Class<T> type, String tableName) {
		boolean setFlag = false, criteriaFlag = false;
		StringBuilder set = new StringBuilder(), criteria = new StringBuilder();
		List<Field> generalFields = new ArrayList<Field>(), idfields = new ArrayList<Field>();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		if (entityMeta == null) {
			Class<?> current = type;
			Set<String> fieldMap = new HashSet<String>();
			while (!Object.class.equals(current)) {
				Field field, declaredFields[] = current.getDeclaredFields();
				for (int i = 0; i < declaredFields.length; i++) {
					field = declaredFields[i];
					String fieldName = field.getName();
					if (!fieldMap.contains(fieldName)) {
						fieldMap.add(fieldName);
						Column column = field.getAnnotation(Column.class);
						if (column != null) {
							field.setAccessible(true);
							String columnName = column.name();
							if (StringUtils.isBlank(columnName)) {
								columnName = StringUtils.camelToUnderline(fieldName, true);
							}
							if (field.getAnnotation(Id.class) == null) {
								generalFields.add(field);
								if (setFlag) {
									set.append(JdbcUtils.COMMA_SPACE);
								} else {
									setFlag = true;
								}
								set.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.PARAM_MARK);
							} else {
								idfields.add(field);
								if (criteriaFlag) {
									criteria.append(JdbcUtils.SPACE_AND_SPACE);
								} else {
									criteriaFlag = true;
								}
								criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE)
										.append(JdbcUtils.COMMA_SPACE);
							}
						}
					}
				}
				current = current.getSuperclass();
			}
		} else {
			List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
			FieldMeta fieldMeta;
			Field field;
			for (int i = 0, size = fieldMetas.size(); i < size; i++) {
				fieldMeta = fieldMetas.get(i);
				field = fieldMeta.getField();
				String columnName = fieldMeta.getColumnName();
				if (fieldMeta.isId()) {
					idfields.add(field);
					if (criteriaFlag) {
						criteria.append(JdbcUtils.SPACE_AND_SPACE);
					} else {
						criteriaFlag = true;
					}
					criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.COMMA_SPACE);
				} else {
					generalFields.add(field);
					if (setFlag) {
						set.append(JdbcUtils.COMMA_SPACE);
					} else {
						setFlag = true;
					}
					set.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.PARAM_MARK);
				}
			}
		}
		if (criteriaFlag) {
			dml.setSql(String.format(UPDATE, tableName, set, criteria));
			generalFields.addAll(idfields);
			dml.setFields(generalFields);
		} else {
			throw new PkNotFoundException(
					"Primary key not found in class ".concat(type.getName()).concat(", please use @Id to config"));
		}
	}

}
