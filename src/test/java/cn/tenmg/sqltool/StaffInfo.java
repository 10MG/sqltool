package cn.tenmg.sqltool;

import java.io.Serializable;

import cn.tenmg.sqltool.config.annotion.Column;
import cn.tenmg.sqltool.config.annotion.Id;

public class StaffInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6026748368660078974L;

	@Id
	@Column
	private String staffId;

	@Column
	private String staffName;
	
	@Column
	private String position;
	
	public StaffInfo() {
		super();
	}

	public StaffInfo(String staffId) {
		super();
		this.staffId = staffId;
	}

	public String getStaffId() {
		return staffId;
	}

	public void setStaffId(String staffId) {
		this.staffId = staffId;
	}

	public String getStaffName() {
		return staffName;
	}

	public void setStaffName(String staffName) {
		this.staffName = staffName;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

}
