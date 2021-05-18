package cn.tenmg.sqltool.sql;

/**
 * SQL相关数据
 * 
 * @author 赵伟均 wjzhao@aliyun.com
 *
 */
public class SQLMetaData {

	/**
	 * WITH子句的位置
	 */
	private int withIndex = -1;

	/**
	 * 可嵌套查询的开始位置
	 */
	private int embedStartIndex = -1;

	/**
	 * SELECT子句的位置
	 */
	private int selectIndex = -1;

	/**
	 * FROM子句的位置
	 */
	private int fromIndex = -1;

	/**
	 * WHER子句的位置
	 */
	private int whereIndex = -1;

	/**
	 * 主查询GROUP BY子句索引
	 */
	private int groupByIndex = -1;

	/**
	 * 主查询ORDER BY子句索引
	 */
	private int orderByIndex = -1;

	/**
	 * 主查询LIMIT子句索引
	 */
	private int limitIndex = -1;

	/**
	 * 主查询OFFSET子句索引
	 */
	private int offsetIndex = -1;

	/**
	 * 可嵌套查询的结束位置
	 */
	private int embedEndIndex = -1;

	/**
	 * SQL的长度
	 */
	private int length = 0;

	public int getWithIndex() {
		return withIndex;
	}

	public void setWithIndex(int withIndex) {
		this.withIndex = withIndex;
	}

	public int getEmbedEndIndex() {
		return embedEndIndex;
	}

	public void setEmbedEndIndex(int embedEndIndex) {
		this.embedEndIndex = embedEndIndex;
	}

	public int getSelectIndex() {
		return selectIndex;
	}

	public void setSelectIndex(int selectIndex) {
		this.selectIndex = selectIndex;
	}

	public int getFromIndex() {
		return fromIndex;
	}

	public void setFromIndex(int fromIndex) {
		this.fromIndex = fromIndex;
	}

	public int getWhereIndex() {
		return whereIndex;
	}

	public void setWhereIndex(int whereIndex) {
		this.whereIndex = whereIndex;
	}

	public int getGroupByIndex() {
		return groupByIndex;
	}

	public void setGroupByIndex(int groupByIndex) {
		this.groupByIndex = groupByIndex;
	}

	public int getOrderByIndex() {
		return orderByIndex;
	}

	public void setOrderByIndex(int orderByIndex) {
		this.orderByIndex = orderByIndex;
	}

	public int getLimitIndex() {
		return limitIndex;
	}

	public void setLimitIndex(int limitIndex) {
		this.limitIndex = limitIndex;
	}

	public int getOffsetIndex() {
		return offsetIndex;
	}

	public void setOffsetIndex(int offsetIndex) {
		this.offsetIndex = offsetIndex;
	}

	public int getEmbedStartIndex() {
		return embedStartIndex;
	}

	public void setEmbedStartIndex(int embedStartIndex) {
		this.embedStartIndex = embedStartIndex;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

}
