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
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JdbcUtils;
import cn.tenmg.sqltool.utils.PlaceHolderUtils;
import cn.tenmg.sqltool.utils.StringUtils;

/**
 * 抽象SQL方言。封装方言基本方法
 * 
 * @author 赵伟均
 *
 */
public abstract class AbstractSQLDialect implements SQLDialect {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5454570237300496297L;

	protected static final String TABLE_NAME = "tableName", COLUMNS = "columns", VALUES = "values", SETS = "sets",
			LEFT_COLUMN_NAME = "columnName", RIGHT_COLUMN_NAME = "columnName";

	/**
	 * 获取额外的SQL模板参数名集，用于初始化SQL模板参数。已初始化的模板参数有columns、values、sets。columns用于表示插入的列，values表示插入的参数，sets表示记录已存在时的更新表达式。这些参数将在后续运行过程中逐步追加字符生成
	 */
	abstract List<String> getExtSQLTemplateParamNames();

	/**
	 * 获取保存数据SQL模板。
	 * 
	 * @return 返回保存数据SQL模板
	 */
	abstract String getSaveSQLTemplate();

	/**
	 * 获取不存在则插入数据SQL模板。
	 * 
	 * @return 返回不存在则插入数据SQL模板
	 */
	abstract String getInsertIfNotExistsSQLTemplate();

	/**
	 * 获取需要添加逗号的参数名列表。处理第2列之前，将需要添加逗号的模板参数均添加逗号
	 * 
	 * @return 返回需要添加逗号的参数名列表
	 */
	abstract List<String> getNeedsCommaParamNames();

	/**
	 * 处理列。根据需要向个各模板参数追加字符串
	 * 
	 * @param columnName
	 *            列名
	 * @param templateParams
	 *            SQL模板参数集
	 */
	abstract void handleColumn(String columnName, Map<String, StringBuilder> templateParams);

	/**
	 * 处理主键列。根据需要向个特定模板参数追加字符串
	 * 
	 * @param columnName
	 *            主键列名
	 * @param templateParams
	 *            SQL模板参数集
	 * @param notFirst
	 *            是否非第一个主键列
	 */
	abstract void handleIdColumn(String columnName, Map<String, StringBuilder> templateParams, boolean notFirst);

	/**
	 * 获取SET字句模板。例如Mysql数据库为{@code ${columnName}=VALUES(${columnName})}
	 * 
	 * @return 返回SET字句模板
	 */
	abstract String getSetTemplate();

	/**
	 * 获取非空时SET字句模板。例如Mysql数据库为<code>${columnName}=IFNULL(VALUES(${columnName}), ${columnName})</code>
	 * 
	 * @return 返回非空时SET字句模板
	 */
	abstract String getSetIfNotNullTemplate();

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
	public <T> MergeSQL save(Class<T> type) {
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(type, templateParams, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (flag) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						flag = true;
					}
					handleColumn(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumn(columnName, templateParams, setsFlag);
					} else {// 记录已存在，组织更新子句
						if (setsFlag) {
							sets.append(JdbcUtils.COMMA_SPACE);
						} else {
							setsFlag = true;
						}
						appendSetIfNotNull(sets, columnName);
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return mergeSql(entityMeta, templateParams);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> MergeSQL save(Class<T> type, String... hardFields) {
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		boolean flag = false;
		EntityMeta entityMeta = getCachedEntityMeta(type);
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(type, templateParams, fieldMetas, hardFieldSet);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					if (flag) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						flag = true;
					}
					handleColumn(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumn(columnName, templateParams, setsFlag);
					} else {// 记录已存在，组织更新子句
						if (setsFlag) {
							sets.append(JdbcUtils.COMMA_SPACE);
						} else {
							setsFlag = true;
						}
						if (hardFieldSet.contains(field.getName())) {
							appendSet(sets, columnName);
						} else {
							appendSetIfNotNull(sets, columnName);
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		if (flag) {
			return mergeSql(entityMeta, templateParams);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> MergeSQL hardSave(Class<T> type) {
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean columnNotFound = false;
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnNotFound = hardParse(type, templateParams, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (columnNotFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnNotFound = true;
					}
					handleColumn(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumn(columnName, templateParams, setsFlag);
					} else {// 记录已存在，组织更新子句
						if (setsFlag) {
							sets.append(JdbcUtils.COMMA_SPACE);
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
			return mergeSql(entityMeta, templateParams);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> SQL save(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(obj, templateParams, params, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				StringBuilder sets = templateParams.get(SETS);
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Object param = fieldMeta.getField().get(obj);
					if (param != null) {// 仅插入非NULL部分的字段值
						params.add(param);
						if (flag) {
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (fieldMeta.isId()) {
							handleIdColumn(columnName, templateParams, setsFlag);
						} else {// 记录已存在，组织更新子句
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
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
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null",
					type.getName()));
		}
	}

	@Override
	public <T> SQL save(T obj, String... hardFields) {
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean flag = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				flag = parse(obj, templateParams, params, fieldMetas, hardFieldSet);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				StringBuilder sets = templateParams.get(SETS);
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					Object param = field.get(obj);
					if (param != null || hardFieldSet.contains(field.getName())) {// 仅插入非NULL或硬保存部分的字段值
						params.add(param);
						if (flag) {
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (fieldMeta.isId()) {
							handleIdColumn(columnName, templateParams, setsFlag);
						} else {// 记录已存在，组织更新子句
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
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
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null or hard save column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null or hard save",
					type.getName()));
		}
	}

	@Override
	public <T> SQL hardSave(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = getCachedEntityMeta(type);
		boolean columnNotFound = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				entityMeta = new EntityMeta();
				entityMeta.setTableName(EntityUtils.getTableName(type));
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnNotFound = hardParse(obj, templateParams, params, fieldMetas);
				entityMeta.setFieldMetas(fieldMetas);
				cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				StringBuilder sets = templateParams.get(SETS);
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					params.add(field.get(obj));
					if (columnNotFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnNotFound = true;
					}
					handleColumn(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumn(columnName, templateParams, setsFlag);
					} else {// 记录已存在，组织更新子句
						if (setsFlag) {
							sets.append(JdbcUtils.COMMA_SPACE);
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
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	private <T> boolean parse(Class<T> type, Map<String, StringBuilder> templateParams, List<FieldMeta> fieldMetas)
			throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Set<String> fieldSet = new HashSet<String>();
		StringBuilder sets = templateParams.get(SETS);
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
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSetIfNotNull(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumn(columnName, templateParams, setsFlag);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private <T> boolean parse(Class<T> type, Map<String, StringBuilder> templateParams, List<FieldMeta> fieldMetas,
			Set<String> hardFieldSet) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Set<String> fieldSet = new HashSet<String>();
		StringBuilder sets = templateParams.get(SETS);
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
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
							fieldMeta.setId(false);
							sets = templateParams.get(SETS);
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							if (hardFieldSet.contains(field.getName())) {
								appendSet(sets, columnName);
							} else {
								appendSetIfNotNull(sets, columnName);
							}
						} else {
							fieldMeta.setId(true);
							handleIdColumn(columnName, templateParams, setsFlag);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private <T> boolean hardParse(Class<T> type, Map<String, StringBuilder> templateParams, List<FieldMeta> fieldMetas)
			throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = type;
		Set<String> fieldSet = new HashSet<String>();
		StringBuilder sets = templateParams.get(SETS);
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
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumn(columnName, templateParams, setsFlag);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	private <T> boolean parse(T obj, Map<String, StringBuilder> templateParams, List<Object> params,
			List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Set<String> fieldSet = new HashSet<String>();
		StringBuilder sets = templateParams.get(SETS);
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
								appendComma(templateParams, getNeedsCommaParamNames());
							} else {
								flag = true;
							}
							handleColumn(columnName, templateParams);
							if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(JdbcUtils.COMMA_SPACE);
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
								handleIdColumn(columnName, templateParams, setsFlag);
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

	private <T> boolean parse(T obj, Map<String, StringBuilder> templateParams, List<Object> params,
			List<FieldMeta> fieldMetas, Set<String> hardFieldSet)
			throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
		StringBuilder sets = templateParams.get(SETS);
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
								appendComma(templateParams, getNeedsCommaParamNames());
							} else {
								flag = true;
							}
							handleColumn(columnName, templateParams);
							if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(JdbcUtils.COMMA_SPACE);
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
								handleIdColumn(columnName, templateParams, setsFlag);
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

	private <T> boolean hardParse(T obj, Map<String, StringBuilder> templateParams, List<Object> params,
			List<FieldMeta> fieldMetas) throws IllegalArgumentException, IllegalAccessException {
		boolean flag = false, setsFlag = false;
		Class<?> current = obj.getClass();
		Map<String, Boolean> fieldMap = new HashMap<String, Boolean>();
		StringBuilder sets = templateParams.get(SETS);
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
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							flag = true;
						}
						handleColumn(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 记录已存在，组织更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JdbcUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumn(columnName, templateParams, setsFlag);
						}
						fieldMetas.add(fieldMeta);
					}
				}
			}
			current = current.getSuperclass();
		}
		return flag;
	}

	/**
	 * 获取SQL模板参数集
	 * 
	 * @return 返回SQL模板参数集
	 */
	private Map<String, StringBuilder> getSQLTemplateParams() {
		Map<String, StringBuilder> params = new HashMap<String, StringBuilder>();
		params.put(COLUMNS, new StringBuilder());
		params.put(VALUES, new StringBuilder());
		params.put(SETS, new StringBuilder());
		List<String> paramNames = getExtSQLTemplateParamNames();
		if (paramNames != null) {
			for (int i = 0, size = paramNames.size(); i < size; i++) {
				params.put(paramNames.get(i), new StringBuilder());
			}
		}
		return params;
	}

	private void appendSet(StringBuilder sets, String columnName) {
		sets.append(PlaceHolderUtils.replace(getSetTemplate(), "columnName", columnName));
	}

	private void appendSetIfNotNull(StringBuilder sets, String columnName) {
		sets.append(PlaceHolderUtils.replace(getSetIfNotNullTemplate(), "columnName", columnName));
	}

	/**
	 * 向参数集指定参数追加逗号
	 * 
	 * @param params
	 *            参数集
	 * @param paramNames
	 *            参数名
	 */
	private static final void appendComma(Map<String, StringBuilder> params, List<String> paramNames) {
		for (int i = 0, size = paramNames.size(); i < size; i++) {
			params.get(paramNames.get(i)).append(JdbcUtils.COMMA_SPACE);
		}
	}

	private MergeSQL mergeSql(EntityMeta entityMeta, Map<String, StringBuilder> templateParams) {
		templateParams.put(TABLE_NAME, new StringBuilder(entityMeta.getTableName()));
		if (templateParams.get(SETS).length() > 0) {
			return new MergeSQL(PlaceHolderUtils.replace(getSaveSQLTemplate(), templateParams),
					entityMeta.getFieldMetas());
		} else {
			return new MergeSQL(PlaceHolderUtils.replace(getInsertIfNotExistsSQLTemplate(), templateParams),
					entityMeta.getFieldMetas());
		}
	}

	private SQL sql(String tableName, Map<String, StringBuilder> templateParams, List<Object> params) {
		templateParams.put(TABLE_NAME, new StringBuilder(tableName));
		if (templateParams.get(SETS).length() > 0) {
			return new SQL(PlaceHolderUtils.replace(getSaveSQLTemplate(), templateParams), params);
		} else {
			return new SQL(PlaceHolderUtils.replace(getInsertIfNotExistsSQLTemplate(), templateParams), params);
		}
	}

}
