package cn.tenmg.sqltool.exception;

/**
 * 未找到主键异常。实体对象没有使用{@code @Id}配置主键列属性会引发此异常
 * 
 * @author June wjzhao@aliyun.com
 *
 * @since 1.0.0
 */
public class PkNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8711952328852873077L;

	public PkNotFoundException() {
		super();
	}

	public PkNotFoundException(String massage) {
		super(massage);
	}

	public PkNotFoundException(Throwable cause) {
		super(cause);
	}

	public PkNotFoundException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
