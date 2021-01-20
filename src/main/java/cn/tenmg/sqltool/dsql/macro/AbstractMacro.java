package cn.tenmg.sqltool.dsql.macro;

import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * 抽象宏，封装宏基本功能
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public abstract class AbstractMacro implements Macro {

	private static class ScriptEngineManagerHolder {
		/**
		 * 脚本引擎管理器
		 */
		protected static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
	}

	public static final ScriptEngineManager getScriptEngineManager() {
		return ScriptEngineManagerHolder.SCRIPT_ENGINE_MANAGER;
	}

	@Override
	public Object excute(String code, Map<String, Object> context, Map<String, Object> params) throws ScriptException {
		ScriptEngine scriptEngine = getScriptEngineManager().getEngineByName("JavaScript");
		if (params != null && !params.isEmpty()) {
			for (Entry<String, Object> entry : params.entrySet()) {
				scriptEngine.put(entry.getKey(), entry.getValue());
			}
		}
		return this.excute(scriptEngine, code, context);
	}

	/**
	 * 执行宏代码。并返回执行结果
	 * 
	 * @param scriptEngine
	 *            脚本引擎
	 * @param code
	 *            宏代码
	 * @param context
	 *            宏运行的上下文
	 * @return
	 * @throws ScriptException
	 */
	abstract Object excute(ScriptEngine scriptEngine, String code, Map<String, Object> context) throws ScriptException;
}
