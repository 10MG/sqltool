package cn.tenmg.sqltool.exception;

/**
 * 创建数据库访问对象异常
 * 
 * @author June wjzhao@aliyun.com
 * @since 1.5.5
 */
public class CreateDaoException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3790123038443901167L;

	public CreateDaoException() {
		super();
	}

	public CreateDaoException(String massage) {
		super(massage);
	}

	public CreateDaoException(Throwable cause) {
		super(cause);
	}

	public CreateDaoException(String massage, Throwable cause) {
		super(massage, cause);
	}

}
