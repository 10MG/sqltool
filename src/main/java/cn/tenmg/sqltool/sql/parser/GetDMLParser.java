package cn.tenmg.sqltool.sql.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.utils.JDBCUtils;
import cn.tenmg.sqltool.utils.StringUtils;

public class GetDMLParser extends AbstractDMLParser {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5532695002201313950L;

	private static final String GET = "SELECT %s FROM %s WHERE %s";

	private static class InstanceHolder {
		private static final GetDMLParser INSTANCE = new GetDMLParser();
	}

	public static final GetDMLParser getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	protected <T> void parseDML(DML dml, Class<T> type, String tableName) {
		Class<?> current = type;
		boolean columnsFlag = false, criteriaFlag = false;
		StringBuilder columns = new StringBuilder(), criteria = new StringBuilder();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
		List<Field> fields = new ArrayList<Field>(), idFields = new ArrayList<Field>();
		while (!Object.class.equals(current)) {
			Field[] declaredFields = current.getDeclaredFields();
			for (int i = 0; i < declaredFields.length; i++) {
				Field field = declaredFields[i];
				String fieldName = field.getName();
				if (!fieldMap.containsKey(fieldName)) {
					fieldMap.put(fieldName, Boolean.TRUE);
					Column column = field.getAnnotation(Column.class);
					if (column != null) {
						field.setAccessible(true);
						fields.add(field);
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						if (field.getAnnotation(Id.class) != null) {
							idFields.add(field);
							if (criteriaFlag) {
								criteria.append(JDBCUtils.SPACE_AND_SPACE);
							} else {
								criteriaFlag = true;
							}
							criteria.append(columnName).append(JDBCUtils.SPACE_EQ_SPACE).append(JDBCUtils.PARAM_MARK);
						}
						if (columnsFlag) {
							columns.append(JDBCUtils.COMMA_SPACE);
						} else {
							columnsFlag = true;
						}
						columns.append(columnName);
					}
				}
			}
			current = current.getSuperclass();
		}
		if (criteriaFlag) {
			dml.setSql(String.format(GET, columns, tableName, criteria));
			dml.setFields(idFields);
		} else {
			throw new PkNotFoundException(
					"Primary key not found in class ".concat(type.getName()).concat(", please use @Id to config"));
		}
	}

}
