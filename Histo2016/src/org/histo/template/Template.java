package org.histo.template;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.model.interfaces.HasID;
import org.histo.util.interfaces.FileHandlerUtil;

import lombok.Getter;
import lombok.Setter;

//@Entity
//@Audited
//@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
//@SelectBeforeUpdate(true)
//@DynamicUpdate(true)
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@SequenceGenerator(name = "template_sequencegenerator", sequenceName = "template_sequence")
@Getter
@Setter
public abstract class Template implements HasID, Cloneable {

	@Id
	@GeneratedValue(generator = "template_sequencegenerator")
	@Column(unique = true, nullable = false)
	protected long id;

	@Column(columnDefinition = "VARCHAR")
	protected String name;

	@Column(columnDefinition = "VARCHAR")
	protected String content;

	@Column(columnDefinition = "VARCHAR")
	protected String content2;

	@Column(columnDefinition = "VARCHAR")
	protected String type;

	@Column
	protected boolean defaultOfType;

	/**
	 * If true the generated content should not be saved in the database
	 */
	@Column
	private boolean transientContent;

	public abstract void prepareTemplate();

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

}