package cn.tenmg.sqltool.dsql.macro;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * else判断宏
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class Else extends AbstractMacro implements Macro {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6840096985687905382L;

	@Override
	Object excute(ScriptEngine scriptEngine, String code, Map<String, Object> context) throws ScriptException {
		Object ifValue = context.get("if");
		if (ifValue == null) {
			return null;
		} else {
			if (ifValue instanceof Boolean) {
				if (((Boolean) ifValue).booleanValue()) {
					return false;
				} else {
					return true;
				}
			}
			return null;
		}
	}

}
