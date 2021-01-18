package cn.tenmg.sqltool.sql.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.DML;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.StringUtils;

public abstract class SQLUtils {

	private static final String SELECT = "SELECT %s FROM %s%s", SPACE_WHERE_SPACE = " WHERE ";

	private static final class DMLCacheHolder {
		private static volatile Map<String, DML> CACHE = new HashMap<String, DML>();
	}

	public static DML getCachedDML(String key) {
		return DMLCacheHolder.CACHE.get(key);
	}

	public static synchronized void cacheDML(String key, DML dml) {
		DMLCacheHolder.CACHE.put(key, dml);
	}

	public static <T> SQL parseSelect(T obj) {
		StringBuilder columns = new StringBuilder(), criteria = new StringBuilder();
		List<Object> params = new ArrayList<Object>();
		boolean hasColumn = false, hasWhere = false;
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		try {
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
								}
								Object param = field.get(obj);
								if (param != null) {
									params.add(param);
									if (hasWhere) {
										criteria.append(JdbcUtils.SPACE_AND_SPACE);
									} else {
										hasWhere = true;
										criteria.append(SPACE_WHERE_SPACE);
									}
									criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE)
											.append(JdbcUtils.PARAM_MARK);
								}
								if (hasColumn) {
									columns.append(JdbcUtils.COMMA_SPACE);
								} else {
									hasColumn = true;
								}
								columns.append(columnName);

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
					Object param = fieldMeta.getField().get(obj);
					if (param != null) {
						params.add(param);
						if (hasWhere) {
							criteria.append(JdbcUtils.SPACE_AND_SPACE);
						} else {
							hasWhere = true;
							criteria.append(SPACE_WHERE_SPACE);
						}
						criteria.append(columnName).append(JdbcUtils.SPACE_EQ_SPACE).append(JdbcUtils.PARAM_MARK);
					}
					if (hasColumn) {
						columns.append(JdbcUtils.COMMA_SPACE);
					} else {
						hasColumn = true;
					}
					columns.append(columnName);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (hasColumn) {
			return new SQL(String.format(SELECT, columns, EntityUtils.getTableName(type), criteria), params);
		} else {
			throw new PkNotFoundException(
					"Column not found in class ".concat(type.getName()).concat(", please use @Column to config"));
		}
	}
}
