package cn.tenmg.sqltool.exception;

/**
 * 转换异常
 * 
 * @author 赵伟均
 * 
 * @since 1.2.0
 */
public class ConvertException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8684977502348790296L;

	public ConvertException() {
		super();
	}

	public ConvertException(String massage) {
		super(massage);
	}

	public ConvertException(Throwable cause) {
		super(cause);
	}

	public ConvertException(String massage, Throwable cause) {
		super(massage, cause);
	}

}
