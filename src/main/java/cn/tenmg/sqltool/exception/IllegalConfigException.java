package cn.tenmg.sqltool.exception;

/**
 * 非法配置异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class IllegalConfigException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4981941372991563546L;

	public IllegalConfigException() {
		super();
	}

	public IllegalConfigException(String massage) {
		super(massage);
	}

	public IllegalConfigException(Throwable cause) {
		super(cause);
	}

	public IllegalConfigException(String massage, Throwable cause) {
		super(massage, cause);
	}

}
