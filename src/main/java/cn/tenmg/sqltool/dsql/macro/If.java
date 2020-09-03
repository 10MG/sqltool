package cn.tenmg.sqltool.dsql.macro;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * if判断宏
 * 
 * @author 赵伟均
 *
 */
public class If extends AbstractMacro implements Macro {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8975289450043764439L;

	@Override
	Object excute(ScriptEngine scriptEngine, String code, Map<String, Object> context) throws ScriptException {
		Object result = scriptEngine.eval(code);
		if (result != null) {
			if (result instanceof Boolean) {
				context.put("if", result);
			} else {
				return null;
			}
		}
		return result;
	}

}
