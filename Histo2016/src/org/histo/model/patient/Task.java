package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Eye;
import org.histo.config.enums.StainingStatus;
import org.histo.config.enums.TaskPriority;
import org.histo.model.Accounting;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DiagnosisStatus;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.StainingInfo;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import org.springframework.core.annotation.Order;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "task_sequencegenerator", sequenceName = "task_sequence")
public class Task implements Parent<Patient>, StainingInfo<Sample>, DiagnosisStatus<DiagnosisRevision>, CreationDate,
		LogAble, ArchivAble {

	public static final int TAB_DIAGNOSIS = 0;
	public static final int TAB_STAINIG = 1;

	public static final byte EYE_RIGHT = 0;
	public static final byte EYE_LEFT = 1;

	private long id;

	private long version;

	/**
	 * Generated Task ID as String
	 */
	private String taskID = "";

	/**
	 * The Patient of the task;
	 */
	private Patient parent;

	/**
	 * Date of creation
	 */
	private long creationDate = 0;

	/**
	 * The date of the sugery
	 */
	private long dateOfSugery = 0;

	/**
	 * Date of reception of the first material
	 */
	private long dateOfReceipt = 0;

	/**
	 * Priority of the task
	 */
	private TaskPriority taskPriority;

	/**
	 * The dueDate
	 */
	private long dueDate = 0;

	/**
	 * Station�r/ambulant/Extern
	 */
	private byte typeOfOperation;

	/**
	 * Details of the case
	 */
	private String caseHistory = "";

	/**
	 * Ward of the patient
	 */
	private String ward = "";

	/**
	 * Ey of the samples right/left/both
	 */
	private Eye eye = Eye.RIGHT;

	/**
	 * Der Task ist archiviert und wird nicht mehr angezeigt wenn true
	 */
	private boolean archived = false;

	/**
	 * all stainings completed
	 */
	private boolean stainingCompleted = false;

	/**
	 * date of staining completion
	 */
	private long stainingCompletionDate = 0;

	/**
	 * True if every diagnosis is finalized
	 */
	private boolean diagnosisCompleted = false;

	/**
	 * Date of diagnosis finalization
	 */
	private long diagnosisCompletionDate = 0;

	/**
	 * True if all persons within the contact list have been notified about the
	 * result.
	 */
	private boolean notificationCompleted = false;

	/**
	 * The date of the completion of the notificaiton.
	 */
	private long notificationCompletionDate = 0;

	/**
	 * Liste aller Personen die �ber die Diangose informiert werden sollen.
	 */
	private List<Contact> contacts;

	/**
	 * List with all samples
	 */
	private List<Sample> samples;

	/**
	 * Element containg all diangnoses
	 */
	private org.histo.model.patient.DiagnosisInfo diagnosisInfo;

	/**
	 * Generated PDFs of this task
	 */
	private List<PDFContainer> attachedPdfs;

	private Accounting accounting;

	private List<Council> council;

	/**
	 * Selected physician to sign the report
	 */
	private Signature physicianSignature;

	/**
	 * Selected consultant to sign the report
	 */
	private Signature consultantSignature;

	private long signatureDate;

	/********************************************************
	 * Transient Variables
	 ********************************************************/
	/**
	 * Die Ausgew�hlte Probe
	 */
	private Sample selectedSample;

	/**
	 * Der Index des TabViews 0 = Diangosen Tab 1 = Objekttr�ger Tab
	 */
	private int tabIndex;

	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	private ArrayList<StainingTableChooser> stainingTableChoosers;

	/**
	 * If set to true, this task is shown in the navigation column on the left
	 * hand side, however there are actions to perform or not.
	 */
	private boolean active;

	/**
	 * True if lazy initialision was successful.
	 */
	private boolean initialized;

	/********************************************************
	 * Transient Variables
	 ********************************************************/

	public Task() {
		// TODO: check if not overwriting the current settings
		setPhysicianSignature(new Signature());
		setConsultantSignature(new Signature());
	}

	/**
	 * Initializes a task with important values.
	 * 
	 * @param parent
	 */
	public Task(Patient parent) {
		this();

		long currentDay = TimeUtil.setDayBeginning(System.currentTimeMillis());
		setCreationDate(currentDay);
		setDateOfReceipt(currentDay);
		setDueDate(currentDay);
		setDateOfSugery(currentDay);

		// 20xx -2000 = tasknumber
		setParent(parent);

	}

	/********************************************************
	 * Transient
	 ********************************************************/
	/**
	 * Updated den Tabindex wenn ein andere Tab (Diagnose oder F�rbung) in der
	 * Gui ausgew�hlt wurde TODO: Remove or use
	 * 
	 * @param event
	 */
	public void onTabChange(TabChangeEvent event) {
		setTabIndex(((TabView) event.getSource()).getIndex());
	}

	/**
	 * Returns a contact marked als primary with the given role.
	 * 
	 * @param contactRole
	 * @return
	 */
	@Transient
	public Contact getPrimaryContact(ContactRole contactRole) {
		for (Contact contact : contacts) {
			if (contact.getRole() == contactRole && contact.isPrimaryContact())
				return contact;
		}

		return null;
	}

	/**
	 * Returns true if either the task is active or a diagnosis or a staining is
	 * needed.
	 * 
	 * @return
	 */
	@Transient
	public boolean isActiveOrActionToPerform() {
		return isActive() || getDiagnosisStatus() == DiagnosisStatus.DIAGNOSIS_NEEDED
				|| getDiagnosisStatus() == DiagnosisStatus.RE_DIAGNOSIS_NEEDED
				|| getStainingStatus() != StainingStatus.PERFORMED;
	}

	@Transient
	public boolean isNewAndActionsPending() {
		if (isNew() && (!isStainingCompleted() || !isDiagnosisCompleted()))
			return true;
		return false;
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "task_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@NotAudited
	public List<Contact> getContacts() {
		if (contacts == null)
			contacts = new ArrayList<Contact>();
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	public List<Sample> getSamples() {
		if (samples == null)
			samples = new ArrayList<Sample>();
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	public List<PDFContainer> getAttachedPdfs() {
		if (attachedPdfs == null)
			attachedPdfs = new ArrayList<PDFContainer>();
		return attachedPdfs;
	}

	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	public long getDateOfReceipt() {
		return dateOfReceipt;
	}

	public void setDateOfReceipt(long dateOfReceipt) {
		this.dateOfReceipt = dateOfReceipt;
	}

	public long getDueDate() {
		return dueDate;
	}

	public void setDueDate(long dueDate) {
		this.dueDate = dueDate;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getDateOfSugery() {
		return dateOfSugery;
	}

	public void setDateOfSugery(long dateOfSugery) {
		this.dateOfSugery = dateOfSugery;
	}

	public byte getTypeOfOperation() {
		return typeOfOperation;
	}

	public void setTypeOfOperation(byte typeOfOperation) {
		this.typeOfOperation = typeOfOperation;
	}

	public String getCaseHistory() {
		return caseHistory;
	}

	public void setCaseHistory(String caseHistory) {
		this.caseHistory = caseHistory;
	}

	@Enumerated(EnumType.STRING)
	public Eye getEye() {
		return eye;
	}

	public void setEye(Eye eye) {
		this.eye = eye;
	}

	public boolean isStainingCompleted() {
		return stainingCompleted;
	}

	public void setStainingCompleted(boolean stainingCompleted) {
		this.stainingCompleted = stainingCompleted;
	}

	public long getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(long stainingCompletionDate) {
		this.stainingCompletionDate = stainingCompletionDate;
	}

	public boolean isDiagnosisCompleted() {
		return diagnosisCompleted;
	}

	public void setDiagnosisCompleted(boolean diagnosisCompleted) {
		this.diagnosisCompleted = diagnosisCompleted;
	}

	public long getDiagnosisCompletionDate() {
		return diagnosisCompletionDate;
	}

	public void setDiagnosisCompletionDate(long diagnosisCompletionDate) {
		this.diagnosisCompletionDate = diagnosisCompletionDate;
	}

	public String getWard() {
		return ward;
	}

	public void setWard(String ward) {
		this.ward = ward;
	}

	@Enumerated(EnumType.ORDINAL)
	public TaskPriority getTaskPriority() {
		return taskPriority;
	}

	public void setTaskPriority(TaskPriority taskPriority) {
		this.taskPriority = taskPriority;
	}

	public boolean isNotificationCompleted() {
		return notificationCompleted;
	}

	public void setNotificationCompleted(boolean notificationCompleted) {
		this.notificationCompleted = notificationCompleted;
	}

	public long getNotificationCompletionDate() {
		return notificationCompletionDate;
	}

	public void setNotificationCompletionDate(long notificationCompletionDate) {
		this.notificationCompletionDate = notificationCompletionDate;
	}

	@OneToMany(fetch = FetchType.LAZY)
	public List<Council> getCouncil() {
		return council;
	}

	public void setCouncil(List<Council> council) {
		this.council = council;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Accounting getAccounting() {
		return accounting;
	}

	public void setAccounting(Accounting accounting) {
		this.accounting = accounting;
	}

	
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("reportOrder ASC")
	public org.histo.model.patient.DiagnosisInfo getDiagnosisInfo() {
		return diagnosisInfo;
	}

	public void setDiagnosisInfo(org.histo.model.patient.DiagnosisInfo diagnosisInfo) {
		this.diagnosisInfo = diagnosisInfo;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Signature getPhysicianSignature() {
		return physicianSignature;
	}

	public void setPhysicianSignature(Signature physicianSignature) {
		this.physicianSignature = physicianSignature;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public Signature getConsultantSignature() {
		return consultantSignature;
	}

	public void setConsultantSignature(Signature consultantSignature) {
		this.consultantSignature = consultantSignature;
	}

	public long getSignatureDate() {
		return signatureDate;
	}

	public void setSignatureDate(long signatureDate) {
		this.signatureDate = signatureDate;
	}

	@Transient
	public Date getSignatureDateAsDate() {
		return new Date(signatureDate);
	}

	public void setSignatureDateAsDate(Date signatureDateAsDate) {
		this.signatureDate = signatureDateAsDate.getTime();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/
	@Transient
	public Date getCreationDateAsDate() {
		return new Date(getCreationDate());
	}

	public void setCreationDateAsDate(Date date) {
		setCreationDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfSugeryAsDate() {
		return new Date(getDateOfSugery());
	}

	public void setDateOfSugeryAsDate(Date date) {
		setDateOfSugery(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfReceiptAsDate() {
		return new Date(getDateOfReceipt());
	}

	public void setDateOfReceiptAsDate(Date date) {
		setDateOfReceipt(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Sample getSelectedSample() {
		return selectedSample;
	}

	public void setSelectedSample(Sample selectedSample) {
		this.selectedSample = selectedSample;
	}

	@Transient
	public ArrayList<StainingTableChooser> getStainingTableRows() {
		return stainingTableChoosers;
	}

	public void setStainingTableRows(ArrayList<StainingTableChooser> stainingTableChoosers) {
		this.stainingTableChoosers = stainingTableChoosers;
	}

	@Transient
	public int getTabIndex() {
		return tabIndex;
	}

	public void setTabIndex(int tabIndex) {
		this.tabIndex = tabIndex;
	}

	@Transient
	public Date getDueDateAsDate() {
		return new Date(getDueDate());
	}

	public void setDueDateAsDate(Date date) {
		setDueDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Transient
	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Returns true if priority is set to TaskPriority.Time
	 */
	@Transient
	public boolean isDueDateSelected() {
		if (getTaskPriority() == TaskPriority.TIME)
			return true;
		return false;
	}

	/**
	 * Sets if the given parameter is true TaskPriority.Time, if false
	 * TaskPriority.NONE
	 * 
	 * @param dueDate
	 */
	public void setDueDateSelected(boolean dueDate) {
		if (dueDate)
			setTaskPriority(TaskPriority.TIME);
		else
			setTaskPriority(TaskPriority.NONE);
	}

	/**
	 * Returns a report with the given type. If no matching record was found
	 * null will be returned.
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public PDFContainer getReport(String type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType().equals(type))
				return pdfContainer;
		}
		return null;
	}

	@Transient
	public PDFContainer getReportByName(String name) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getName().contains(name))
				return pdfContainer;
		}
		return null;
	}

	/**
	 * Adds a report to the report list
	 * 
	 * @return
	 */
	@Transient
	public void addReport(PDFContainer pdfTemplate) {
		getAttachedPdfs().add(pdfTemplate);
	}

	/**
	 * Removes a report with a specific type from the database
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public PDFContainer removeReport(String type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType().equals(type)) {
				getAttachedPdfs().remove(pdfContainer);
				return pdfContainer;
			}
		}
		return null;
	}

	/**
	 * Returns true if a diagnosis is marked as malign.
	 * 
	 * @return
	 */
	@Transient
	public boolean isMalign() {
		for (DiagnosisRevision diagnosisRevision : getReports()) {
			if (diagnosisRevision.isMalign())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/

	/**
	 * Checks if all staings are performed an returns true if the status has
	 * changed. If no change occurred false will be returned.
	 * 
	 * @return
	 */
	@Transient
	public boolean updateStainingStatus() {
		if (getStainingStatus() == StainingStatus.PERFORMED) {
			if (!isStainingCompleted()) {
				setStainingCompleted(true);
				setStainingCompletionDate(System.currentTimeMillis());
				return true;
			}
		} else {
			if (isStainingCompleted()) {
				setStainingCompleted(false);
				setStainingCompletionDate(0);
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if all slides are staind and stets the allStainingsPerformed flag
	 * in the task object to true.
	 * 
	 * @param sample
	 */
	@Transient
	public static final boolean checkIfAllSlidesAreStained(Task task) {
		if (task.getStainingStatus() == StainingStatus.PERFORMED) {
			task.setStainingCompleted(true);
			task.setStainingCompletionDate(System.currentTimeMillis());
		} else
			task.setStainingCompleted(false);

		return task.getStainingStatus() == StainingStatus.PERFORMED ? true : false;
	}

	/********************************************************
	 * Interface DiagnosisStatusState
	 ********************************************************/
	/**
	 * Overwrites the {@link DiagnosisStatusState} interfaces, and returns the status
	 * of the diagnoses.
	 */
	@Override
	@Transient
	public DiagnosisStatus getDiagnosisStatus() {
		return getDiagnosisStatus(getReports());
	}

	/********************************************************
	 * Interface DiagnosisStatusState
	 ********************************************************/

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/
	/**
	 * Overwrites the {@link StainingInfo} interfaces new method. Returns true
	 * if the creation date was on the same as the current day.
	 */
	@Override
	@Transient
	public boolean isNew() {
		return isNew(getCreationDate());
	}

	/**
	 * Returns the status of the staining process. Either it can return staining
	 * performed, staining needed, restaining needed (restaining is returned if
	 * at least one staining is marked as restaining).
	 */
	@Override
	@Transient
	public StainingStatus getStainingStatus() {
		return getStainingStatus(getSamples());
	}

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne(targetEntity = Patient.class)
	public Patient getParent() {
		return parent;
	}

	public void setParent(Patient parent) {
		this.parent = parent;
	}

	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Setzt alle Kinder
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt Kinder
		for (Sample sample : getSamples()) {
			sample.setArchived(archived);
		}
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt die TaskID als
	 * identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getTaskID();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zur�ck
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.TASK_ARCHIV;
	}
	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/

}
