package cn.tenmg.sqltool.dsql;

import java.io.Serializable;
import java.util.List;

public class JdbcSql implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8160135968550136566L;

	/**
	 * SQL
	 */
	private String script;

	/**
	 * 参数
	 */
	private List<Object> params;

	public JdbcSql() {
		super();
	}

	public JdbcSql(String script, List<Object> params) {
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

	public List<Object> getParams() {
		return params;
	}

	public void setParams(List<Object> params) {
		this.params = params;
	}

}
