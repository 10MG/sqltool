package cn.tenmg.sqltool.exception;

/**
 * 无合适的SQL引擎异常。找不到合适的SQL引擎会引发此异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class NoSuitableSqlEngineException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1719250710633071189L;

	public NoSuitableSqlEngineException() {
		super();
	}

	public NoSuitableSqlEngineException(String massage) {
		super(massage);
	}

	public NoSuitableSqlEngineException(Throwable cause) {
		super(cause);
	}

	public NoSuitableSqlEngineException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
