package cn.tenmg.sqltool.config.annotion;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 实体类属性对应数据库表列注解
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface Column {

	/**
	 * <p>
	 * 列名（可选配置）
	 * </p>
	 * 默认为属性名的下划线表示方式。例如fieldName属性的默认列名为FIELD_NAME
	 */
	String name() default "";

}
