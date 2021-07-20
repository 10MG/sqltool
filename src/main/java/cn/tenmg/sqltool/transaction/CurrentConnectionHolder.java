package cn.tenmg.sqltool.transaction;

import java.sql.Connection;

/**
 * 当前连接持有者
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 * 
 * @since 1.2.0
 */
public abstract class CurrentConnectionHolder {

	private static ThreadLocal<Connection> currentConnection = new ThreadLocal<Connection>();

	public static void set(Connection con) {
		currentConnection.set(con);
	}

	public static Connection get() {
		return currentConnection.get();
	}

	public static void remove() {
		currentConnection.remove();
	}
}
