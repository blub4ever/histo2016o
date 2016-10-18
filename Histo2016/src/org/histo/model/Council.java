package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "council_sequencegenerator", sequenceName = "council_sequence")
public class Council {
	private long id;

	private long version;

	private Physician councilPhysician;
	
	private Physician physicianRequestingCouncil;
	
	private String councilText;
	
	private String attachment;
	
	@Id
	@GeneratedValue(generator = "council_sequencegenerator")
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

	@OneToOne
	public Physician getCouncilPhysician() {
		return councilPhysician;
	}

	public void setCouncilPhysician(Physician councilPhysician) {
		this.councilPhysician = councilPhysician;
	}

	@OneToOne
	public Physician getPhysicianRequestingCouncil() {
		return physicianRequestingCouncil;
	}

	public void setPhysicianRequestingCouncil(Physician physicianRequestingCouncil) {
		this.physicianRequestingCouncil = physicianRequestingCouncil;
	}

	@Column(columnDefinition = "text")
	public String getCouncilText() {
		return councilText;
	}

	public void setCouncilText(String councilText) {
		this.councilText = councilText;
	}

	@Column(columnDefinition = "text")
	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}

	
	
}