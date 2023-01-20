package cn.tenmg.sqltool.sql.executer;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.tenmg.dsl.utils.ObjectUtils;
import cn.tenmg.dsl.utils.StringUtils;
import cn.tenmg.sqltool.exception.SQLExecutorException;
import cn.tenmg.sqltool.sql.ResultGetter;
import cn.tenmg.sqltool.sql.SQLExecuter;
import cn.tenmg.sqltool.sql.utils.ResultGetterUtils;

/**
 * 只读SQL执行器
 * 
 * @author June wjzhao@aliyun.com
 *
 * @param <T>
 *            返回结果类型
 * 
 * @since 1.2.0
 */
public abstract class ReadOnlySQLExecuter<T> implements SQLExecuter<T> {

	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * 获取结果当前行集指定列的值
	 * 
	 * @param rs
	 *            结果集
	 * @param columnIndex
	 *            指定列索引
	 * @param type
	 *            值的类型
	 * @return 返回当前行集指定列的值
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	protected static <T> T getRow(ResultSet rs, Class<T> type) throws SQLException {
		T row;
		ResultGetter<?> resultGetter = ResultGetterUtils.getResultGetter(type);
		if (resultGetter == null) {
			if (Map.class.isAssignableFrom(type)) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				List<String> feildNames = new ArrayList<String>();
				for (int i = 1; i <= columnCount; i++) {
					feildNames.add(StringUtils.toCamelCase(rsmd.getColumnLabel(i), "_", false));
				}
				try {
					row = type.getConstructor().newInstance();
					for (int i = 1; i <= columnCount; i++) {
						((Map<String, Object>) row).put(feildNames.get(i - 1), rs.getObject(i));
					}
				} catch (Exception e) {
					throw new SQLExecutorException(e);
				}
			} else if (List.class.isAssignableFrom(type)) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				try {
					row = type.getConstructor().newInstance();
					for (int i = 1; i <= columnCount; i++) {
						((List<Object>) row).add(rs.getObject(i));
					}
				} catch (Exception e) {
					throw new SQLExecutorException(e);
				}
			} else if (Ref.class.isAssignableFrom(type)) {
				row = (T) rs.getRef(1);
			} else if (Array.class.isAssignableFrom(type)) {
				row = (T) rs.getArray(1);
			} else if (SQLXML.class.isAssignableFrom(type)) {
				row = (T) rs.getSQLXML(1);
			} else if (Blob.class.isAssignableFrom(type)) {
				row = (T) rs.getBlob(1);
			} else if (Clob.class.isAssignableFrom(type)) {
				row = (T) rs.getClob(1);
			} else if (NClob.class.isAssignableFrom(type)) {
				row = (T) rs.getNClob(1);
			} else if (RowId.class.isAssignableFrom(type)) {
				row = (T) rs.getRowId(1);
			} else if (InputStream.class.isAssignableFrom(type)) {
				row = (T) rs.getBinaryStream(1);
			} else if (Reader.class.isAssignableFrom(type)) {
				row = (T) rs.getCharacterStream(1);
			} else {
				Constructor<T> constructor = null;
				try {
					constructor = type.getConstructor();
				} catch (Exception e) {
				}
				if (constructor == null) {
					return (T) rs.getObject(1);
				}
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				List<String> fieldNames = new ArrayList<String>(columnCount);
				for (int i = 1; i <= columnCount; i++) {
					String attribute = StringUtils.toCamelCase(rsmd.getColumnLabel(i), "_", false);
					fieldNames.add(attribute);
				}
				try {
					row = constructor.newInstance();
					for (int i = 1; i <= columnCount; i++) {
						setValue(row, fieldNames.get(i - 1), rs, i);
					}
				} catch (Exception e) {
					throw new SQLExecutorException(e);
				}
			}
		} else {
			row = (T) resultGetter.getValue(rs, 1);
		}
		return row;
	}

	public static <T> void setValue(Object row, String fieldName, ResultSet rs, int columnIndex) throws Exception {
		Class<?> type = ObjectUtils.getFieldType(row, fieldName, false); // 获取设置字段值时所需的数据类型
		Object value;
		if (type == null) {// 无法识别准确类型
			value = rs.getObject(columnIndex);
		} else {
			ResultGetter<?> resultGetter = ResultGetterUtils.getResultGetter(type);
			if (resultGetter == null) {// 没有定义该类型结果获取器，则进一步判断类型再调用不同API
				if (Ref.class.isAssignableFrom(type)) {
					value = rs.getRef(columnIndex);
				} else if (Array.class.isAssignableFrom(type)) {
					value = rs.getArray(columnIndex);
				} else if (SQLXML.class.isAssignableFrom(type)) {
					value = rs.getSQLXML(columnIndex);
				} else if (Blob.class.isAssignableFrom(type)) {
					value = rs.getBlob(columnIndex);
				} else if (Clob.class.isAssignableFrom(type)) {
					value = rs.getClob(columnIndex);
				} else if (NClob.class.isAssignableFrom(type)) {
					value = rs.getNClob(columnIndex);
				} else if (RowId.class.isAssignableFrom(type)) {
					value = rs.getRowId(columnIndex);
				} else if (InputStream.class.isAssignableFrom(type)) {
					value = rs.getBinaryStream(columnIndex);
				} else if (Reader.class.isAssignableFrom(type)) {
					value = rs.getCharacterStream(columnIndex);
				} else {
					value = rs.getObject(columnIndex);
				}
			} else {// 已定义该类型结果获取器，则直接调用结果获取器的API
				value = resultGetter.getValue(rs, columnIndex);
			}
		}
		ObjectUtils.setValue(row, fieldName, value, false);
	}

	public static void main(String[] args) throws NoSuchMethodException, SecurityException {
		Object object = new Integer(1);
		object.getClass().getConstructor();
	}

}