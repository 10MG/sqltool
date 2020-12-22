package cn.tenmg.sqltool.exception;

/**
 * 数据访问异常。通过反射访问或设置属性引发异常时会抛出次异常
 * 
 * @author 赵伟均
 *
 */
public class DataAccessException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5819754179912275832L;

	public DataAccessException() {
		super();
	}

	public DataAccessException(String massage) {
		super(massage);
	}

	public DataAccessException(Throwable cause) {
		super(cause);
	}

	public DataAccessException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
