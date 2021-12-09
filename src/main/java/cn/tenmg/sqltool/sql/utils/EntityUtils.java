package cn.tenmg.sqltool.sql.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.dsl.Script;
import cn.tenmg.dsl.utils.StringUtils;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.config.annotion.Table;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * 实体工具类
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.3.0
 */
public abstract class EntityUtils {

	private static final String SELECT_SQL_TPL = "SELECT %s FROM %s%s", SPACE_WHERE_SPACE = " WHERE ";

	private static final class EntityMetaCacheHolder {
		private static volatile Map<Class<?>, EntityMeta> CACHE = new HashMap<Class<?>, EntityMeta>();
	}

	public static EntityMeta getCachedEntityMeta(Class<?> type) {
		return EntityMetaCacheHolder.CACHE.get(type);
	}

	public static synchronized void cacheEntityMeta(Class<?> type, EntityMeta entityMeta) {
		EntityMetaCacheHolder.CACHE.put(type, entityMeta);
	}

	public static final String getTableName(Class<?> type) {
		Table table = type.getAnnotation(Table.class);
		String tableName;
		if (table != null) {
			tableName = table.name();
		} else {
			tableName = StringUtils.camelToUnderline(type.getSimpleName(), true);
		}
		return tableName;
	}

	public static <T> Script<List<Object>> parseSelect(T obj) {
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
										criteria.append(JDBCExecuteUtils.SPACE_AND_SPACE);
									} else {
										hasWhere = true;
										criteria.append(SPACE_WHERE_SPACE);
									}
									criteria.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
											.append(SQLUtils.PARAM_MARK);
								}
								if (hasColumn) {
									columns.append(JDBCExecuteUtils.COMMA_SPACE);
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
							criteria.append(JDBCExecuteUtils.SPACE_AND_SPACE);
						} else {
							hasWhere = true;
							criteria.append(SPACE_WHERE_SPACE);
						}
						criteria.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE).append(SQLUtils.PARAM_MARK);
					}
					if (hasColumn) {
						columns.append(JDBCExecuteUtils.COMMA_SPACE);
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
			return new Script<List<Object>>(String.format(SELECT_SQL_TPL, columns, EntityUtils.getTableName(type), criteria), params);
		} else {
			throw new PkNotFoundException(
					"Column not found in class ".concat(type.getName()).concat(", please use @Column to config"));
		}
	}

	/**
	 * 从实体对象中获取属性参数集
	 * 
	 * @param obj
	 *            实体对象
	 * @param fields
	 *            参数属性集
	 * @return 返回参数集
	 */
	public static <T> List<Object> getParams(T obj, List<Field> fields) {
		List<Object> params = new ArrayList<Object>();
		for (int i = 0, size = fields.size(); i < size; i++) {
			try {
				params.add(fields.get(i).get(obj));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new DataAccessException(e);
			}
		}
		return params;
	}

}
