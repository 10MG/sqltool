package cn.tenmg.sqltool.data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果模型
 * 
 * @author June wjzhao@laiyun.com
 *
 * @param <T>
 *            行数据类型
 * @since 1.2.0
 */
public class Page<T extends Serializable> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7540584530038648088L;

	private int pageSize;

	private long currentPage;

	private Long total;

	private Long totalPage;

	private List<T> rows;

	public Page() {
	}

	public Page(long currentPage) {
		this.currentPage = currentPage;
	}

	public Page(long currentPage, int pageSize) {
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public long getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(long currentPage) {
		this.currentPage = currentPage;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public Long getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(Long totalPage) {
		this.totalPage = totalPage;
	}

	public List<T> getRows() {
		return rows;
	}

	public void setRows(List<T> rows) {
		this.rows = rows;
	}

}