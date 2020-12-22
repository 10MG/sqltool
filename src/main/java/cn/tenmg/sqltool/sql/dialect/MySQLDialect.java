package cn.tenmg.sqltool.sql.dialect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.ColumnNotFoundException;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.sql.JdbcSql;
import cn.tenmg.sqltool.sql.MergeSql;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * Mysql 方言
 * 
 * @author 赵伟均
 *
 */
public class MySQLDialect implements SQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7189284927835898553L;

	private static final String INSERT_IGNORE = "INSERT IGNORE INTO %s (%s) VALUES (%s)";

	private static final String SAVE = "INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s";

	private static class InstanceHolder {
		private static final MySQLDialect INSTANCE = new MySQLDialect();
	}

	public static final MySQLDialect getInstance() {
		return InstanceHolder.INSTANCE;
	}

	private static final class EntityMetaCacheHolder {
		private static volatile Map<Class<?>, EntityMeta> CACHE = new HashMap<Class<?>, EntityMeta>();
	}

	protected static EntityMeta getCachedEntityMeta(Class<?> type) {
		return EntityMetaCacheHolder.CACHE.get(type);
	}

	protected static synchronized void cacheEntityMeta(Class<?> type, EntityMeta entityMeta) {
		EntityMetaCacheHolder.CACHE.put(type, entityMeta);
	}

	@Override
	public <T> MergeSql save(Class<T> type) {
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(type, columns, values, sets, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (flag) {
						appendComma(columns, values);
					} else {
						flag = true;
					}
					appendColumnAndParam(columns, values, columnName);
					if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
						if (setsFlag) {
							sets.append(", ");
						} else {
							setsFlag = true;
						}
						appendSetIfNotnull(sets, columnName);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return mergeSql(entityMeta, columns, values, sets);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> MergeSql save(Class<T> type, String... hardFields) {
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(type, columns, values, sets, fieldMetas, hardFieldSet);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					if (flag) {
						appendComma(columns, values);
					} else {
						flag = true;
					}
					appendColumnAndParam(columns, values, columnName);
					if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
						if (setsFlag) {
							sets.append(", ");
						} else {
							setsFlag = true;
						}
						if (hardFieldSet.contains(field.getName())) {
							appendSet(sets, columnName);
						} else {
							appendSetIfNotnull(sets, columnName);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return mergeSql(entityMeta, columns, values, sets);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> MergeSql hardSave(Class<T> type) {
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean columnNotFound = false;
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnNotFound = hardParse(type, columns, values, sets, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (columnNotFound) {
						appendComma(columns, values);
					} else {
						columnNotFound = true;
					}
					appendColumnAndParam(columns, values, columnName);
					if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
						if (setsFlag) {
							sets.append(", ");
						} else {
							setsFlag = true;
						}
						appendSet(sets, columnName);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (columnNotFound) {
			return mergeSql(entityMeta, columns, values, sets);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> JdbcSql save(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		List<Object> params = new ArrayList<Object>();
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(obj, columns, values, sets, params, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Object param = fieldMeta.getField().get(obj);
					if (param != null) {// 仅插入非NULL部分的字段值
						params.add(param);
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return jdbcSql(entityMeta.getTableName(), columns, values, sets, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null",
					type.getName()));
		}
	}

	@Override
	public <T> JdbcSql save(T obj, String... hardFields) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		List<Object> params = new ArrayList<Object>();
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(obj, columns, values, sets, params, fieldMetas, hardFieldSet);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					Object param = field.get(obj);
					if (param != null || hardFieldSet.contains(field.getName())) {// 仅插入非NULL或硬保存部分的字段值
						params.add(param);
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return jdbcSql(entityMeta.getTableName(), columns, values, sets, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null or hard save column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null or hard save",
					type.getName()));
		}
	}

	@Override
	public <T> JdbcSql hardSave(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean columnNotFound = false;
		List<Object> params = new ArrayList<Object>();
		StringBuilder columns = new StringBuilder(), values = new StringBuilder(), sets = new StringBuilder();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnNotFound = hardParse(obj, columns, values, sets, params, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					params.add(field.get(obj));
					if (columnNotFound) {
						appendComma(columns, values);
					} else {
						columnNotFound = true;
					}
					appendColumnAndParam(columns, values, columnName);
					if (!fieldMeta.isId()) {// ON DUPLICATE KEY UPDATE部分
						if (setsFlag) {
							sets.append(", ");
						} else {
							setsFlag = true;
						}
						appendSet(sets, columnName);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (columnNotFound) {
			return jdbcSql(entityMeta.getTableName(), columns, values, sets, params);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	private static final <T> boolean parse(Class<T> type, StringBuilder columns, StringBuilder values,
			StringBuilder sets, List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Set<String> fieldSet = new HashSet<String>();
		while (!Object.class.equals(current)) {
			Field[] declaredFields = current.getDeclaredFields();
			for (int i = 0; i < declaredFields.length; i++) {
				Field field = declaredFields[i];
				String fieldName = field.getName();
				if (!fieldSet.contains(fieldName)) {
					fieldSet.add(fieldName);
					Column column = field.getAnnotation(Column.class);
					if (column != null) {
						field.setAccessible(true);
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							appendSetIfNotnull(sets, columnName);
						} else {
							fieldMeta.setId(true);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final <T> boolean parse(Class<T> type, StringBuilder columns, StringBuilder values,
			StringBuilder sets, List<FieldMeta> fieldMetas, Set<String> hardFieldSet)
			throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
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
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							if (hardFieldSet.contains(field.getName())) {
								appendSet(sets, columnName);
							} else {
								appendSetIfNotnull(sets, columnName);
							}
						} else {
							fieldMeta.setId(true);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final <T> boolean hardParse(Class<T> type, StringBuilder columns, StringBuilder values,
			StringBuilder sets, List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
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
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final <T> boolean parse(T obj, StringBuilder columns, StringBuilder values, StringBuilder sets,
			List<Object> params, List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Set<String> fieldSet = new HashSet<String>();
		while (!Object.class.equals(current)) {
			Field[] declaredFields = current.getDeclaredFields();
			for (int i = 0; i < declaredFields.length; i++) {
				Field field = declaredFields[i];
				String fieldName = field.getName();
				if (!fieldSet.contains(fieldName)) {
					fieldSet.add(fieldName);
					Column column = field.getAnnotation(Column.class);
					if (column != null) {
						field.setAccessible(true);
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						Object param = field.get(obj);
						if (param != null) {// 仅插入非NULL部分的字段值
							params.add(param);
							if (flag) {
								appendComma(columns, values);
							} else {
								flag = true;
							}
							appendColumnAndParam(columns, values, columnName);
							if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(", ");
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
							}
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final <T> boolean parse(T obj, StringBuilder columns, StringBuilder values, StringBuilder sets,
			List<Object> params, List<FieldMeta> fieldMetas, Set<String> hardFieldSet)
			throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
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
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						Object param = field.get(obj);
						if (param != null || hardFieldSet.contains(field.getName())) {// 仅插入非NULL或硬保存部分的字段值
							params.add(param);
							if (flag) {
								appendComma(columns, values);
							} else {
								flag = true;
							}
							appendColumnAndParam(columns, values, columnName);
							if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(", ");
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
							}
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final <T> boolean hardParse(T obj, StringBuilder columns, StringBuilder values, StringBuilder sets,
			List<Object> params, List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
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
						String columnName = column.name();
						if (StringUtils.isBlank(columnName)) {
							columnName = StringUtils.camelToUnderline(fieldName, true);
						}
						FieldMeta fieldMeta = new FieldMeta(field, columnName);
						params.add(field.get(obj));
						if (flag) {
							appendComma(columns, values);
						} else {
							flag = true;
						}
						appendColumnAndParam(columns, values, columnName);
						if (field.getAnnotation(Id.class) == null) {// ON DUPLICATE KEY UPDATE部分
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(", ");
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private static final void appendComma(StringBuilder columns, StringBuilder values) {
		columns.append(", ");
		values.append(", ");
	}

	private static final void appendColumnAndParam(StringBuilder columns, StringBuilder values, String columnName) {
		columns.append(columnName);
		values.append("?");
	}

	private static final void appendSet(StringBuilder sets, String columnName) {
		sets.append(columnName).append(" = VALUES(").append(columnName).append(")");
	}

	private static final void appendSetIfNotnull(StringBuilder sets, String columnName) {
		sets.append(columnName).append(" = IFNULL(VALUES(").append(columnName).append("), ").append(columnName)
				.append(")");
	}

	private static final MergeSql mergeSql(EntityMeta entityMeta, StringBuilder columns, StringBuilder values,
			StringBuilder sets) {
		if (sets.length() > 0) {
			return new MergeSql(String.format(SAVE, entityMeta.getTableName(), columns, values, sets),
					entityMeta.getFieldMetas());
		} else {
			return new MergeSql(String.format(INSERT_IGNORE, entityMeta.getTableName(), columns, values),
					entityMeta.getFieldMetas());
		}
	}

	private static final JdbcSql jdbcSql(String tableName, StringBuilder columns, StringBuilder values,
			StringBuilder sets, List<Object> params) {
		if (sets.length() > 0) {
			return new JdbcSql(String.format(SAVE, tableName, columns, values, sets), params);
		} else {
			return new JdbcSql(String.format(INSERT_IGNORE, tableName, columns, values), params);
		}
	}

}
