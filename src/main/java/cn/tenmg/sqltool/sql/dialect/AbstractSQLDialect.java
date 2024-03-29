package cn.tenmg.sqltool.sql.dialect;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.tenmg.dsl.Script;
import cn.tenmg.dsl.utils.PlaceHolderUtils;
import cn.tenmg.dsl.utils.StringUtils;
import cn.tenmg.sql.paging.SQLMetaData;
import cn.tenmg.sql.paging.SQLPagingDialect;
import cn.tenmg.sql.paging.utils.SQLUtils;
import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;
import cn.tenmg.sqltool.exception.ColumnNotFoundException;
import cn.tenmg.sqltool.exception.DataAccessException;
import cn.tenmg.sqltool.exception.NoColumnForUpdateException;
import cn.tenmg.sqltool.exception.PkNotFoundException;
import cn.tenmg.sqltool.sql.MergeSQL;
import cn.tenmg.sqltool.sql.SQLDialect;
import cn.tenmg.sqltool.sql.UpdateSQL;
import cn.tenmg.sqltool.sql.meta.EntityMeta;
import cn.tenmg.sqltool.sql.meta.FieldMeta;
import cn.tenmg.sqltool.sql.utils.EntityUtils;
import cn.tenmg.sqltool.utils.JDBCExecuteUtils;

/**
 * 抽象SQL方言。封装方言基本方法
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.1.0
 */
public abstract class AbstractSQLDialect implements SQLDialect {

	private static final String UPDATE_SET_TEMPLATE = "${columnName} = ?",
			UPDATE = "UPDATE ${tableName} SET ${sets} WHERE ${condition}";

	protected static final String TABLE_NAME = "tableName", COLUMNS = "columns", VALUES = "values", SETS = "sets",
			LEFT_COLUMN_NAME = "columnName", RIGHT_COLUMN_NAME = "columnName";

	protected static final int SELECT_LEN = "SELECT".length();

	/**
	 * 获取SQL分页查询方言
	 * 
	 * @return SQL分页查询方言
	 */
	abstract SQLPagingDialect getSQLPagingDialect();

	/**
	 * 获取更新语句非空时SET子句模板。例如Mysql数据库为<code>${columnName}=IFNULL(${columnName}, ?)</code>
	 * 
	 * @return 返回非空时SET子句模板
	 */
	abstract String getUpdateSetIfNotNullTemplate();

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
	 * 保存时每个列的处理方法。根据需要向个各模板参数追加字符串
	 * 
	 * @param columnName
	 *            列名
	 * @param templateParams
	 *            SQL模板参数集
	 */
	abstract void handleColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams);

	/**
	 * 保存时每个主键列的处理方法。根据需要向个特定模板参数追加字符串
	 * 
	 * @param columnName
	 *            主键列名
	 * @param templateParams
	 *            SQL模板参数集
	 * @param notFirst
	 *            是否非第一个主键列
	 */
	abstract void handleIdColumnWhenSave(String columnName, Map<String, StringBuilder> templateParams,
			boolean notFirst);

	/**
	 * 获取SET子句模板。例如Mysql数据库为<code>${columnName}=VALUES(${columnName})</code>
	 * 
	 * @return 返回SET子句模板
	 */
	abstract String getSetTemplate();

	/**
	 * 获取非空时SET子句模板。例如Mysql数据库为<code>${columnName}=IFNULL(VALUES(${columnName}), ${columnName})</code>
	 * 
	 * @return 返回非空时SET子句模板
	 */
	abstract String getSetIfNotNullTemplate();

	@Override
	public String countSql(String namedSql, SQLMetaData sqlMetaData) {
		return getSQLPagingDialect().countSql(namedSql, sqlMetaData);
	}

	@Override
	public String pageSql(Connection con, String sql, Map<String, ?> params, SQLMetaData sqlMetaData, int pageSize,
			long currentPage) throws SQLException {
		return getSQLPagingDialect().pageSql(con, sql, params, sqlMetaData, pageSize, currentPage);
	}

	@Override
	public <T> UpdateSQL update(Class<T> type) {
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean hasId = false, hasGeneralColumn = false;
		StringBuilder sets = new StringBuilder(), condition = new StringBuilder();
		List<Field> generalFields = new ArrayList<Field>(), idFields = new ArrayList<Field>();
		try {
			if (entityMeta == null) {
				Class<?> current = type;
				Set<String> fieldSet = new HashSet<String>();
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
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
								if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
									fieldMeta.setId(false);
									generalFields.add(field);
									if (hasGeneralColumn) {
										sets.append(JDBCExecuteUtils.COMMA_SPACE);
									} else {
										hasGeneralColumn = true;
									}
									sets.append(PlaceHolderUtils.replace(getUpdateSetIfNotNullTemplate(), "columnName",
											columnName));
								} else {
									fieldMeta.setId(true);
									idFields.add(field);
									if (hasId) {
										condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
									} else {
										hasId = true;
									}
									condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
											.append(SQLUtils.PARAM_MARK);
								}
								fieldMetas.add(fieldMeta);
							}
						}
					}
					current = current.getSuperclass();
				}
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					Field field = fieldMeta.getField();
					String columnName = fieldMeta.getColumnName();
					if (fieldMeta.isId()) {
						idFields.add(field);
						if (hasId) {
							condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
						} else {
							hasId = true;
						}
						condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
								.append(SQLUtils.PARAM_MARK);
					} else {// 组织已存在时的更新子句
						generalFields.add(field);
						if (hasGeneralColumn) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
						} else {
							hasGeneralColumn = true;
						}
						sets.append(
								PlaceHolderUtils.replace(getUpdateSetIfNotNullTemplate(), "columnName", columnName));
					}
				}
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(e);
		}
		return updateSQL(type, hasId, hasGeneralColumn, entityMeta.getTableName(), sets, condition, generalFields,
				idFields);
	}

	@Override
	public <T> UpdateSQL update(Class<T> type, String... hardFields) {
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean hasId = false, hasGeneralColumn = false;
		StringBuilder sets = new StringBuilder(), condition = new StringBuilder();
		List<Field> generalFields = new ArrayList<Field>(), idFields = new ArrayList<Field>();
		String updateSetTemplate;
		try {
			if (entityMeta == null) {
				Class<?> current = type;
				Set<String> fieldSet = new HashSet<String>();
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
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
								if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
									fieldMeta.setId(false);
									generalFields.add(field);
									if (hasGeneralColumn) {
										sets.append(JDBCExecuteUtils.COMMA_SPACE);
									} else {
										hasGeneralColumn = true;
									}
									if (hardFieldSet.contains(field.getName())) {
										updateSetTemplate = UPDATE_SET_TEMPLATE;
									} else {
										updateSetTemplate = getUpdateSetIfNotNullTemplate();
									}
									sets.append(PlaceHolderUtils.replace(updateSetTemplate, "columnName", columnName));
								} else {
									fieldMeta.setId(true);
									idFields.add(field);
									if (hasId) {
										condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
									} else {
										hasId = true;
									}
									condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
											.append(SQLUtils.PARAM_MARK);
								}
								fieldMetas.add(fieldMeta);
							}
						}
					}
					current = current.getSuperclass();
				}
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					Field field = fieldMeta.getField();
					String columnName = fieldMeta.getColumnName();
					if (fieldMeta.isId()) {
						idFields.add(field);
						if (hasId) {
							condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
						} else {
							hasId = true;
						}
						condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
								.append(SQLUtils.PARAM_MARK);
					} else {// 组织已存在时的更新子句
						generalFields.add(field);
						if (hasGeneralColumn) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
						} else {
							hasGeneralColumn = true;
						}
						if (hardFieldSet.contains(field.getName())) {
							updateSetTemplate = UPDATE_SET_TEMPLATE;
						} else {
							updateSetTemplate = getUpdateSetIfNotNullTemplate();
						}
						sets.append(PlaceHolderUtils.replace(updateSetTemplate, "columnName", columnName));
					}
				}
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(e);
		}
		return updateSQL(type, hasId, hasGeneralColumn, entityMeta.getTableName(), sets, condition, generalFields,
				idFields);
	}

	@Override
	public <T> MergeSQL save(Class<T> type) {
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean columnFound = false;
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = parse(type, templateParams, fieldMetas);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (columnFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnFound = true;
					}
					handleColumnWhenSave(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumnWhenSave(columnName, templateParams, setsFlag);
					} else {// 组织已存在时的更新子句
						if (setsFlag) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
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
		boolean columnFound = false;
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = parse(type, templateParams, fieldMetas, hardFieldSet);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					if (columnFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnFound = true;
					}
					handleColumnWhenSave(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumnWhenSave(columnName, templateParams, setsFlag);
					} else {// 组织已存在时的更新子句
						if (setsFlag) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
			return mergeSql(entityMeta, templateParams);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> MergeSQL hardSave(Class<T> type) {
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean columnFound = false;
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = hardParse(type, templateParams, fieldMetas);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				StringBuilder sets = templateParams.get(SETS);
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					if (columnFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnFound = true;
					}
					handleColumnWhenSave(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumnWhenSave(columnName, templateParams, setsFlag);
					} else {// 组织已存在时的更新子句
						if (setsFlag) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
			return mergeSql(entityMeta, templateParams);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	@Override
	public <T> Script<List<Object>> update(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean hasId = false, hasGeneralColumn = false;
		StringBuilder sets = new StringBuilder(), condition = new StringBuilder();
		List<Object> values = new ArrayList<Object>(), conditionValues = new ArrayList<Object>();
		try {
			if (entityMeta == null) {
				Class<?> current = type;
				Set<String> fieldSet = new HashSet<String>();
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
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
								if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
									fieldMeta.setId(false);
									if (param != null) {
										values.add(param);
										if (hasGeneralColumn) {
											sets.append(JDBCExecuteUtils.COMMA_SPACE);
										} else {
											hasGeneralColumn = true;
										}
										sets.append(PlaceHolderUtils.replace(UPDATE_SET_TEMPLATE, "columnName",
												columnName));
									}
								} else {
									fieldMeta.setId(true);
									conditionValues.add(param);
									if (hasId) {
										condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
									} else {
										hasId = true;
									}
									condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
											.append(SQLUtils.PARAM_MARK);
								}
								fieldMetas.add(fieldMeta);
							}
						}
					}
					current = current.getSuperclass();
				}
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					Field field = fieldMeta.getField();
					String columnName = fieldMeta.getColumnName();
					Object param = field.get(obj);
					if (fieldMeta.isId()) {
						conditionValues.add(param);
						if (hasId) {
							condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
						} else {
							hasId = true;
						}
						condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
								.append(SQLUtils.PARAM_MARK);
					} else {// 组织已存在时的更新子句
						if (param != null) {
							values.add(param);
							if (hasGeneralColumn) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
							} else {
								hasGeneralColumn = true;
							}
							sets.append(PlaceHolderUtils.replace(UPDATE_SET_TEMPLATE, "columnName", columnName));
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		return sql(obj, hasId, hasGeneralColumn, entityMeta.getTableName(), sets, condition, values, conditionValues);
	}

	@Override
	public <T> Script<List<Object>> update(T obj, String... hardFields) {
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean hasId = false, hasGeneralColumn = false;
		StringBuilder sets = new StringBuilder(), condition = new StringBuilder();
		List<Object> values = new ArrayList<Object>(), conditionValues = new ArrayList<Object>();
		try {
			if (entityMeta == null) {
				Class<?> current = type;
				Set<String> fieldSet = new HashSet<String>();
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
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
								if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
									fieldMeta.setId(false);
									if (param != null || hardFieldSet.contains(field.getName())) {
										values.add(param);
										if (hasGeneralColumn) {
											sets.append(JDBCExecuteUtils.COMMA_SPACE);
										} else {
											hasGeneralColumn = true;
										}
										sets.append(PlaceHolderUtils.replace(UPDATE_SET_TEMPLATE, "columnName",
												columnName));
									}
								} else {
									fieldMeta.setId(true);
									conditionValues.add(param);
									if (hasId) {
										condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
									} else {
										hasId = true;
									}
									condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
											.append(SQLUtils.PARAM_MARK);
								}
								fieldMetas.add(fieldMeta);
							}
						}
					}
					current = current.getSuperclass();
				}
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					Field field = fieldMeta.getField();
					String columnName = fieldMeta.getColumnName();
					Object param = field.get(obj);
					if (fieldMeta.isId()) {
						conditionValues.add(param);
						if (hasId) {
							condition.append(JDBCExecuteUtils.SPACE_AND_SPACE);
						} else {
							hasId = true;
						}
						condition.append(columnName).append(JDBCExecuteUtils.SPACE_EQ_SPACE)
								.append(SQLUtils.PARAM_MARK);
					} else {// 组织已存在时的更新子句
						if (param != null || hardFieldSet.contains(field.getName())) {
							values.add(param);
							if (hasGeneralColumn) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
							} else {
								hasGeneralColumn = true;
							}
							sets.append(PlaceHolderUtils.replace(UPDATE_SET_TEMPLATE, "columnName", columnName));
						}
					}
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new DataAccessException(e);
		}
		return sql(obj, hasId, hasGeneralColumn, entityMeta.getTableName(), sets, condition, values, conditionValues);
	}

	@Override
	public <T> Script<List<Object>> save(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean columnFound = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = parse(obj, templateParams, params, fieldMetas);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
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
						if (columnFound) {
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							columnFound = true;
						}
						handleColumnWhenSave(columnName, templateParams);
						if (fieldMeta.isId()) {
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
						} else {// 组织已存在时的更新子句
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null",
					type.getName()));
		}
	}

	@Override
	public <T> Script<List<Object>> save(T obj, String... hardFields) {
		Set<String> hardFieldSet = new HashSet<String>();
		for (int i = 0; i < hardFields.length; i++) {
			hardFieldSet.add(hardFields[i]);
		}
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean columnFound = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = parse(obj, templateParams, params, fieldMetas, hardFieldSet);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
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
						if (columnFound) {
							appendComma(templateParams, getNeedsCommaParamNames());
						} else {
							columnFound = true;
						}
						handleColumnWhenSave(columnName, templateParams);
						if (fieldMeta.isId()) {
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
						} else {// 组织已存在时的更新子句
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(String.format(
					"Not null or hard save column not found in class %s, please use @Column to config fields and make sure at lease one of them is not null or hard save",
					type.getName()));
		}
	}

	@Override
	public <T> Script<List<Object>> hardSave(T obj) {
		Class<?> type = obj.getClass();
		EntityMeta entityMeta = EntityUtils.getCachedEntityMeta(type);
		boolean columnFound = false;
		List<Object> params = new ArrayList<Object>();
		Map<String, StringBuilder> templateParams = getSQLTemplateParams();
		try {
			if (entityMeta == null) {
				List<FieldMeta> fieldMetas = new ArrayList<FieldMeta>();
				columnFound = hardParse(obj, templateParams, params, fieldMetas);
				entityMeta = new EntityMeta(EntityUtils.getTableName(type), fieldMetas);
				EntityUtils.cacheEntityMeta(type, entityMeta);
			} else {
				boolean setsFlag = false;
				List<FieldMeta> fieldMetas = entityMeta.getFieldMetas();
				StringBuilder sets = templateParams.get(SETS);
				for (int i = 0, size = fieldMetas.size(); i < size; i++) {
					FieldMeta fieldMeta = fieldMetas.get(i);
					String columnName = fieldMeta.getColumnName();
					Field field = fieldMeta.getField();
					params.add(field.get(obj));
					if (columnFound) {
						appendComma(templateParams, getNeedsCommaParamNames());
					} else {
						columnFound = true;
					}
					handleColumnWhenSave(columnName, templateParams);
					if (fieldMeta.isId()) {
						handleIdColumnWhenSave(columnName, templateParams, setsFlag);
					} else {// 组织已存在时的更新子句
						if (setsFlag) {
							sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
		if (columnFound) {
			return sql(entityMeta.getTableName(), templateParams, params);
		} else {
			throw new ColumnNotFoundException(
					String.format("Column not found in class %s, please use @Column to config fields", type.getName()));
		}
	}

	private <T> UpdateSQL updateSQL(Class<T> type, boolean hasId, boolean hasGeneralColumn, String tableName,
			StringBuilder sets, StringBuilder condition, List<Field> generalFields, List<Field> idFields) {
		if (hasId) {
			if (hasGeneralColumn) {
				generalFields.addAll(idFields);
				return new UpdateSQL(
						PlaceHolderUtils.replace(UPDATE, "tableName", tableName, "sets", sets, "condition", condition),
						generalFields);
			} else {
				throw new NoColumnForUpdateException(String.format(
						"There is only id column(s), but no general column found in class %s, please check your config",
						type.getName()));
			}
		} else {
			throw new PkNotFoundException(
					"Primary key not found in class ".concat(type.getName()).concat(", please use @Id to config"));
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
						handleColumnWhenSave(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSetIfNotNull(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
						handleColumnWhenSave(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
							fieldMeta.setId(false);
							sets = templateParams.get(SETS);
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
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
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
						handleColumnWhenSave(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
							handleColumnWhenSave(columnName, templateParams);
							if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(JDBCExecuteUtils.COMMA_SPACE);
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
								handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
							handleColumnWhenSave(columnName, templateParams);
							if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
								fieldMeta.setId(false);
								if (setsFlag) {
									sets.append(JDBCExecuteUtils.COMMA_SPACE);
								} else {
									setsFlag = true;
								}
								appendSet(sets, columnName);
							} else {
								fieldMeta.setId(true);
								handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
						handleColumnWhenSave(columnName, templateParams);
						if (field.getAnnotation(Id.class) == null) {// 组织已存在时的更新子句
							fieldMeta.setId(false);
							if (setsFlag) {
								sets.append(JDBCExecuteUtils.COMMA_SPACE);
							} else {
								setsFlag = true;
							}
							appendSet(sets, columnName);
						} else {
							fieldMeta.setId(true);
							handleIdColumnWhenSave(columnName, templateParams, setsFlag);
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
			params.get(paramNames.get(i)).append(JDBCExecuteUtils.COMMA_SPACE);
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

	private <T> Script<List<Object>> sql(T obj, boolean hasId, boolean hasGeneralColumn, String tableName,
			StringBuilder sets, StringBuilder condition, List<Object> values, List<Object> conditionValues) {
		if (hasId) {
			if (hasGeneralColumn) {
				values.addAll(conditionValues);
				return new Script<List<Object>>(
						PlaceHolderUtils.replace(UPDATE, "tableName", tableName, "sets", sets, "condition", condition),
						values);
			} else {
				throw new NoColumnForUpdateException(String.format(
						"There is only id column(s), but no general column witch is not null found in object %s, please check your config and field value",
						obj.toString()));
			}
		} else {
			throw new PkNotFoundException("Primary key not found in class ".concat(obj.getClass().getName())
					.concat(", please use @Id to config"));
		}
	}

	private Script<List<Object>> sql(String tableName, Map<String, StringBuilder> templateParams, List<Object> params) {
		templateParams.put(TABLE_NAME, new StringBuilder(tableName));
		if (templateParams.get(SETS).length() > 0) {
			return new Script<List<Object>>(PlaceHolderUtils.replace(getSaveSQLTemplate(), templateParams), params);
		} else {
			return new Script<List<Object>>(PlaceHolderUtils.replace(getInsertIfNotExistsSQLTemplate(), templateParams),
					params);
		}
	}

}
