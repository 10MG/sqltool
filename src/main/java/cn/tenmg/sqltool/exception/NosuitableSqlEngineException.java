package cn.tenmg.sqltool.exception;

/**
 * 无合适的SQL引擎异常。找不到合适的SQL引擎会引发此异常
 * 
 * @author 赵伟均
 *
 */
public class NosuitableSqlEngineException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1719250710633071189L;

	public NosuitableSqlEngineException() {
		super();
	}

	public NosuitableSqlEngineException(String massage) {
		super(massage);
	}

	public NosuitableSqlEngineException(Throwable cause) {
		super(cause);
	}

	public NosuitableSqlEngineException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
