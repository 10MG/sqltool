package cn.tenmg.sqltool.sql.parser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.exception.ColumnNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

public class InsertDMLParser extends AbstractDMLParser {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1072900553731732141L;

	private static final String INSERT = "INSERT INTO %s(%s) VALUES (%s)";

	private static class InstanceHolder {
		private static final InsertDMLParser INSTANCE = new InsertDMLParser();
	}

	public static final InsertDMLParser getInstance() {
		return InstanceHolder.INSTANCE;
	}

	@Override
	protected <T> void parseDML(DML dml, Class<T> type, String tableName) {
		Class<?> current = type;
		boolean flag = false;
		StringBuilder columns = new StringBuilder(), values = new StringBuilder();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
		List<Field> fields = new ArrayList<Field>();
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
						if (flag) {
							columns.append(JdbcUtils.COMMA_SPACE);
							values.append(JdbcUtils.COMMA_SPACE);
						} else {
							flag = true;
						}
						columns.append(columnName);
						values.append(JdbcUtils.PARAM_MARK);
					}
				}
			}
			current = current.getSuperclass();
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
