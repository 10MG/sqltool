package cn.tenmg.sqltool.exception;

/**
 * 非法调用异常。不合理的调用顺序会引发此一场
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public class IllegalCallException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6136502662630490330L;

	public IllegalCallException() {
		super();
	}

	public IllegalCallException(String massage) {
		super(massage);
	}

	public IllegalCallException(Throwable cause) {
		super(cause);
	}

	public IllegalCallException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
