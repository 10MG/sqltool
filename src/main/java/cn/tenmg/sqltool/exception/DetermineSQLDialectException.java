package cn.tenmg.sqltool.exception;

/**
 * 确定SQL方言异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class DetermineSQLDialectException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6398191142449912872L;

	public DetermineSQLDialectException() {
		super();
	}

	public DetermineSQLDialectException(String massage) {
		super(massage);
	}

	public DetermineSQLDialectException(Throwable cause) {
		super(cause);
	}

	public DetermineSQLDialectException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
