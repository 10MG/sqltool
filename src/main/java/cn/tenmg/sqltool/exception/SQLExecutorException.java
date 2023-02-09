package cn.tenmg.sqltool.exception;

/**
 * SQL执行异常
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.5.0
 */
public class SQLExecutorException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5523191114712061655L;

	public SQLExecutorException() {
		super();
	}

	public SQLExecutorException(String massage) {
		super(massage);
	}

	public SQLExecutorException(Throwable cause) {
		super(cause);
	}

	public SQLExecutorException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
