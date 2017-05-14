package org.histo.model.interfaces;

import org.histo.model.patient.Patient;

public interface PatientRollbackAble extends HasID {

	public Patient getPatient();

	/**
	 * Returns a hierarchical path for logging the object
	 * 
	 * @return
	 */
	public default String getLogPath() {
		return "";
	}
}
