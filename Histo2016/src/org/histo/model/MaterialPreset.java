package org.histo.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.model.interfaces.EditAbleEntity;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.interfaces.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "materialPreset_sequencegenerator", sequenceName = "materialPreset_sequence")
public class MaterialPreset implements EditAbleEntity<MaterialPreset>, ListOrder<MaterialPreset>, LogAble, HasID {

	@Expose
	private long id;

	@Expose
	private String name;

	@Expose
	private String commentary;

	@Expose
	private List<StainingPrototype> stainingPrototypes;

	@Expose
	private int indexInList;

	public MaterialPreset() {
	}

	public MaterialPreset(MaterialPreset stainingPrototypeList) {
		this.id = stainingPrototypeList.getId();
		update(stainingPrototypeList);
	}

	@Override
	public String toString() {
		return "ID: " + getId() + " Name: " + getName();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "materialPreset_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(columnDefinition = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	public List<StainingPrototype> getStainingPrototypes() {
		if (stainingPrototypes == null)
			stainingPrototypes = new ArrayList<StainingPrototype>();

		return stainingPrototypes;
	}

	public void setStainingPrototypes(List<StainingPrototype> stainingPrototypes) {
		this.stainingPrototypes = stainingPrototypes;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface ListOrder
	 ********************************************************/
	@Column
	public int getIndexInList() {
		return indexInList;
	}

	public void setIndexInList(int indexInList) {
		this.indexInList = indexInList;
	}

	/********************************************************
	 * Interface ListOrder
	 ********************************************************/

	@Transient
	public void update(MaterialPreset stainingPrototypeList) {
		this.name = stainingPrototypeList.getName();
		this.commentary = stainingPrototypeList.getCommentary();
		this.stainingPrototypes = new ArrayList<StainingPrototype>(stainingPrototypeList.getStainingPrototypes());
	}

	@Transient
	public String asGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

}
