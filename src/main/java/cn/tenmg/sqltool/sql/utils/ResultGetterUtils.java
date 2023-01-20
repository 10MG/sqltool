package cn.tenmg.sqltool.sql.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

import cn.tenmg.sqltool.sql.ResultGetter;

/**
 * 结果获取器工具类
 * 
 * @author June wjzhao@aliyun.com
 *
 */
@SuppressWarnings("rawtypes")
public abstract class ResultGetterUtils {

	private static final Map<Class<?>, ResultGetter> RESULT_GETTERS = new HashMap<Class<?>, ResultGetter>();

	static {
		ServiceLoader<ResultGetter> loader = ServiceLoader.load(ResultGetter.class);
		ResultGetter<?> resultGetter;
		for (Iterator<ResultGetter> it = loader.iterator(); it.hasNext();) {
			resultGetter = it.next();
			RESULT_GETTERS.put(resultGetter.getType(), resultGetter);
		}
	}

	public static ResultGetter getResultGetter(Class<?> type) {
		return RESULT_GETTERS.get(type);
	}

}
