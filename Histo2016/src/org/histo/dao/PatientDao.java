package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.model.Log;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	public void getAllPaitent() {
		Person p = new Person();
		System.out.println(getSession().save(p));
	}

	public void getPatitentByDate(long from, long to) {
		Criteria c = getSession().createCriteria(Person.class, "pat");
		c.createAlias("pat.tasks", "tasks"); // inner join by default
		c.add(Restrictions.gt("tasks.taskOccoured", from)).add(Restrictions.lt("tasks.taskOccoured", to)).list();
	}

	/**
	 * Returns a list of useres with the given piz. At least 6 numbers of the
	 * piz are needed.
	 * 
	 * @param piz
	 * @return
	 */
	public List<Patient> searchForPatientsPiz(String piz) {
		Criteria c = getSession().createCriteria(Patient.class);
		String regex = "";
		if (piz.length() != 8) {
			regex = "[0-9]{" + (8 - piz.length()) + "}";
		}
		c.add(Restrictions.like("piz", piz + regex));
		return c.list();
	}

	/**
	 * Returns a patient object for a specific piz. The piz has to be 8
	 * characters long.
	 * 
	 * @param piz
	 * @return
	 */
	public Patient searchForPatientPiz(String piz) {
		if (piz.length() != 8)
			return null;

		Criteria c = getSession().createCriteria(Patient.class);
		c.add(Restrictions.eq("piz", piz));
		List<Patient> result = c.list();
		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

	public List<Patient> searchForPatientPizes(List<String> piz) {
		Criteria c = getSession().createCriteria(Patient.class);
		c.add(Restrictions.in("piz", piz));
		return c.list();
	}

	public List<Patient> getPatientWithoutTasks(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.add(Restrictions.ge("patient.creationDate", fromDate)).add(Restrictions.le("patient.creationDate", toDate));
		c.add(Restrictions.isEmpty("patient.tasks"));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientByAddDateToWorklist(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.add(Restrictions.ge("patient.creationDate", fromDate)).add(Restrictions.le("patient.creationDate", toDate));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientBySampleCreationDateBetweenDates(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.tasks", "_tasks");
		c.createAlias("_tasks.samples", "_samples");
		c.add(Restrictions.ge("_samples.creationDate", fromDate)).add(Restrictions.le("_samples.creationDate", toDate));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientByStainingsBetweenDates(long fromDate, long toDate, boolean completed) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.creationDate", fromDate)).add(Restrictions.le("_tasks.creationDate", toDate));
		query.add(Restrictions.eq("_tasks.stainingCompleted", completed));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return query.getExecutableCriteria(getSession()).list();
	}

	public List<Patient> getPatientByDiagnosBetweenDates(long fromDate, long toDate, boolean completed) {
		DetachedCriteria query = DetachedCriteria.forClass(Patient.class, "patient");

		query.createAlias("patient.tasks", "_tasks");
		query.add(Restrictions.ge("_tasks.stainingCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.stainingCompletionDate", toDate));
		query.add(Restrictions.eq("_tasks.stainingCompleted", true));
		query.add(Restrictions.eq("_tasks.diagnosisCompleted", completed));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
	}

	public List<Patient> getWorklistDynamicallyByType(long fromDate, long toDate, WorklistSearchFilter filter) {
		switch (filter) {
		case ADDED_TO_WORKLIST:
			return getPatientByAddDateToWorklist(fromDate, toDate);
		case TASK_CREATION:
			return getPatientBySampleCreationDateBetweenDates(fromDate, toDate);
		case STAINING_COMPLETED:
			return getPatientByStainingsBetweenDates(fromDate, toDate, true);
		case DIAGNOSIS_COMPLETED:
			return getPatientByDiagnosBetweenDates(fromDate, toDate, true);
		default:
			return null;
		}
	}

	public List<Patient> getPatientsByNameSurnameDateExcludePiz(String name, String surname, Date date,
			List<String> pizesToExclude) {

		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.person", "_person");

		if (name != null && !name.isEmpty())
			c.add(Restrictions.ilike("_person.name", name, MatchMode.ANYWHERE));
		if (surname != null & !surname.isEmpty())
			c.add(Restrictions.ilike("_person.surname", surname, MatchMode.ANYWHERE));
		if (date != null)
			c.add(Restrictions.eq("_person.birthday", date));
		if (pizesToExclude != null && !pizesToExclude.isEmpty())
			c.add(Restrictions.not(Restrictions.in("piz", pizesToExclude.toArray())));

		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Patient> result = (List<Patient>) c.list();
		return result != null ? result : new ArrayList<>();
	}
}
