package cn.tenmg.sqltool.dsql.macro;

import java.util.Map;

import javax.script.ScriptException;

/**
 * 宏
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 */
public interface Macro {
	/**
	 * 执行宏并返回计算结果
	 * 
	 * @param code
	 *            宏逻辑代码
	 * 
	 * @param context
	 *            宏运行的上下文
	 * @param params
	 *            宏运行的参数
	 * @return 返回可执行结构化查询语言（SQL）的片段
	 */
	Object excute(String code, Map<String, Object> context, Map<String, Object> params) throws ScriptException;
}
