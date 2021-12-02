package cn.tenmg.sqltool.exception;

/**
 * 事务异常
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public class TransactionException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2081897494890685994L;

	public TransactionException() {
		super();
	}

	public TransactionException(String massage) {
		super(massage);
	}

	public TransactionException(Throwable cause) {
		super(cause);
	}

	public TransactionException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
