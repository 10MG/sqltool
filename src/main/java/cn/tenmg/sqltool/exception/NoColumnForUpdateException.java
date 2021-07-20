package cn.tenmg.sqltool.exception;

/**
 * 没有需要更新的列异常。当实体仅含主键列不含普通列时会引发此异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 * @since 1.1.1
 */
public class NoColumnForUpdateException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8058296526770455550L;

	public NoColumnForUpdateException() {
		super();
	}

	public NoColumnForUpdateException(String massage) {
		super(massage);
	}

	public NoColumnForUpdateException(Throwable cause) {
		super(cause);
	}

	public NoColumnForUpdateException(String massage, Throwable cause) {
		super(massage, cause);
	}

}
