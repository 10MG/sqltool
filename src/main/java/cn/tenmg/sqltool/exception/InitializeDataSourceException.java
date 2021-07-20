package cn.tenmg.sqltool.exception;

/**
 * 初始化数据源异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 * @since 1.2.0
 */
public class InitializeDataSourceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1540068122804061758L;

	public InitializeDataSourceException() {
		super();
	}

	public InitializeDataSourceException(String massage) {
		super(massage);
	}

	public InitializeDataSourceException(Throwable cause) {
		super(cause);
	}

	public InitializeDataSourceException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
