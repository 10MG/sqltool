package cn.tenmg.sqltool.exception;

/**
 * 无合适的SQL方言异常。找不到合适的SQL方言会引发此异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class NoSuitableSQLDialectExeption extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1096220289612512140L;

	public NoSuitableSQLDialectExeption() {
		super();
	}

	public NoSuitableSQLDialectExeption(String massage) {
		super(massage);
	}

	public NoSuitableSQLDialectExeption(Throwable cause) {
		super(cause);
	}

	public NoSuitableSQLDialectExeption(String massage, Throwable cause) {
		super(massage, cause);
	}
}
