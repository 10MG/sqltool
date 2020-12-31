package cn.tenmg.sqltool.dsql;

import java.io.Serializable;
import java.util.Map;

/**
 * 使用命名参数的SQL对象模型
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class NamedSQL implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3876821753500865601L;

	/**
	 * SQL
	 */
	private String script;

	/**
	 * 参数
	 */
	private Map<String, Object> params;

	public NamedSQL() {
		super();
	}

	public NamedSQL(String script) {
		super();
		this.script = script;
	}

	public NamedSQL(String script, Map<String, Object> params) {
		super();
		this.script = script;
		this.params = params;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

}
