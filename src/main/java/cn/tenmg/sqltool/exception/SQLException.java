package cn.tenmg.sqltool.exception;

/**
 * SQL异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class SQLException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7902196651045931813L;

	public SQLException() {
		super();
	}

	public SQLException(String massage) {
		super(massage);
	}

	public SQLException(Throwable cause) {
		super(cause);
	}

	public SQLException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
