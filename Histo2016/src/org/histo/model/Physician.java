package org.histo.model;

import javax.persistence.Basic;
import javax.persistence.Entity;

import com.google.gson.annotations.Expose;

@Entity
public class Physician extends Person {

	private static final long serialVersionUID = 7358147861813210904L;

	/**
	 * Full name Title, Name, Surname
	 */
	@Expose
	private String fullName;
	
	/**
	 * Pager Number
	 */
	@Expose
	private String pager;
	
	/**
	 * clinic internal title
	 */
	@Expose
	private String clinicTitle;
	
	/**
	 * clinic internal department
	 */
	@Expose
	private String department;

	/**
	 * Is surgeon
	 */
	@Expose
	private boolean roleSurgeon;

	/**
	 * Is external doctor
	 */
	@Expose
	private boolean roleResidentDoctor;

	/**
	 * Is internal doctor or clinical personnel
	 */
	@Expose
	private boolean roleClinicDoctor;
	
	/**
	 * Other
	 */
	@Expose
	private boolean roleMiscellaneous;
	
	@Basic
	public boolean isRoleSurgeon() {
		return roleSurgeon;
	}

	public void setRoleSurgeon(boolean roleSurgeon) {
		this.roleSurgeon = roleSurgeon;
	}

	@Basic
	public boolean isRoleResidentDoctor() {
		return roleResidentDoctor;
	}

	public void setRoleResidentDoctor(boolean roleResidentDoctor) {
		this.roleResidentDoctor = roleResidentDoctor;
	}

	@Basic
	public boolean isRoleClinicDoctor() {
		return roleClinicDoctor;
	}

	public void setRoleClinicDoctor(boolean roleClinicDoctor) {
		this.roleClinicDoctor = roleClinicDoctor;
	}

	@Basic
	public boolean isRoleMiscellaneous() {
		return roleMiscellaneous;
	}

	public void setRoleMiscellaneous(boolean roleMiscellaneous) {
		this.roleMiscellaneous = roleMiscellaneous;
	}

	public String getPager() {
		return pager;
	}

	public void setPager(String pager) {
		this.pager = pager;
	}


	public String getClinicTitle() {
	    return clinicTitle;
	}

	public void setClinicTitle(String clinicTitle) {
	    this.clinicTitle = clinicTitle;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
}