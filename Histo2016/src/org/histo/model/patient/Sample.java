package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import org.histo.config.enums.DiagnosisStatus;
import org.histo.config.enums.DiagnosisType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.StainingStatus;
import org.histo.model.MaterialPreset;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DiagnosisInfo;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.StainingInfo;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
public class Sample implements Parent<Task>, StainingInfo<Block>, CreationDate, LogAble, ArchivAble {

	private long id;

	private long version;

	/**
	 * Parent of this sample.
	 */
	private Task parent;

	/**
	 * Sample ID as string
	 */
	private String sampleID = "";

	/**
	 * Date of sample creation
	 */
	private long creationDate = 0;

	/**
	 * If true the not completed stainings are restainings.
	 */
	private boolean reStainingPhase = false;

	/**
	 * blocks array
	 */
	private List<Block> blocks;

	/**
	 * Wenn archived true ist, wird dieser sample nicht mehr angezeigt
	 */
	private boolean archived;

	/**
	 * Material name is first initialized with the name of the typeOfMaterial.
	 * Can be later changed.
	 */
	private String material = "";

	/**
	 * Material object, containing preset for staining
	 */
	private MaterialPreset materilaPreset;

	public Sample() {
	}

	/**
	 * Constructor for adding this sample to a task.
	 * 
	 * @param task
	 */
	public Sample(Task task) {
		this(task, null, true);
	}

	/**
	 * Constructor for adding this sample to a task and set the material as
	 * well.
	 * 
	 * @param task
	 */
	public Sample(Task task, MaterialPreset material, boolean useAutoNomenclature) {
		setCreationDate(System.currentTimeMillis());
		setParent(task);
		setMaterilaPreset(material);
		setMaterial(material == null ? "" : material.getName());
		task.getSamples().add(this);

		updateNameOfSample(useAutoNomenclature);

	}

	/**
	 * Generates a sample name, if useAutoNomenclature is true an name will be
	 * auto generated
	 * 
	 * @param useAutoNomenclature
	 */
	public void updateNameOfSample(boolean useAutoNomenclature) {
		if (useAutoNomenclature)
			setSampleID(TaskUtil.getRomanNumber(getParent().getSamples().size() + 1));
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "sample_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("blockID ASC")
	public List<Block> getBlocks() {
		if (blocks == null)
			blocks = new ArrayList<Block>();
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	@Basic
	public String getSampleID() {
		return sampleID;
	}

	public void setSampleID(String sampleID) {
		this.sampleID = sampleID;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Basic
	public boolean isReStainingPhase() {
		return reStainingPhase;
	}

	public void setReStainingPhase(boolean reStainingPhase) {
		this.reStainingPhase = reStainingPhase;
	}

	@Basic
	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	@OneToOne
	@NotAudited
	public MaterialPreset getMaterilaPreset() {
		return materilaPreset;
	}

	public void setMaterilaPreset(MaterialPreset materilaPreset) {
		this.materilaPreset = materilaPreset;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/******************************************************** Transient ********************************************************/

	/******************************************************** Transient ********************************************************/

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
		return getStainingStatus(getBlocks());
	}

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne(targetEntity = Task.class)
	public Task getParent() {
		return parent;
	}

	public void setParent(Task parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Setzt alle Kinder
	 */
	@Basic
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt alle Kinder
		for (Block block : getBlocks()) {
			block.setArchived(archived);
		}
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt die SampleID als
	 * identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getSampleID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zurück
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.SAMPLE_ARCHIV;
	}
	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
}
