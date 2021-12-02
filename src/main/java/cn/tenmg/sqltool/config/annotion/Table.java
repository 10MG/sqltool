package cn.tenmg.sqltool.config.annotion;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 实体类对应数据库表注解
 * 
 * @author June wjzhao@aliyun.com
 * 
 * @since 1.0.0
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {
	/**
	 * <p>
	 * 表名（可选配置）
	 * </p>
	 * 默认为类名的下划线表示方式。例如DemoTable类的默认表名为DEMO_TABLE
	 */
	String name() default "";
}