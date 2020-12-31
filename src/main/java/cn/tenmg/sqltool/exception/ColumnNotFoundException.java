package cn.tenmg.sqltool.exception;

/**
 * 未找到列异常。实体类未使用{@code @Column}注解数据表的列属性时会引发此异常
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class ColumnNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1049475731838144594L;

	public ColumnNotFoundException() {
		super();
	}

	public ColumnNotFoundException(String massage) {
		super(massage);
	}

	public ColumnNotFoundException(Throwable cause) {
		super(cause);
	}

	public ColumnNotFoundException(String massage, Throwable cause) {
		super(massage, cause);
	}
}
