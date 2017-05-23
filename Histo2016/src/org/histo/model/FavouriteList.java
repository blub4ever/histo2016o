package org.histo.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.interfaces.HasID;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "favouritelist_sequencegenerator", sequenceName = "favouritelist_sequence")
public class FavouriteList implements HasID {

	public static final int StainingList_ID = 1;
	public static final int DiagnosisList_ID = 2;
	public static final int NotificationList_ID = 3;
	
	public static final int ReStainingList_ID = 4;
	public static final int ReDiagnosisList_ID = 5;
	
	public static final int StayInStainingList_ID = 6;
	public static final int StayInDiagnosisList_ID = 7;
	
	
	private long id;
	private HistoUser owner;
	private String name;

	private boolean defaultList;
	private boolean editAble;
	private boolean global;

	private List<FavouriteListItem> items;

	
	
	@Override
	public String toString() {
		return "ID: " + getId() + ", Name: " +getName(); 
	}

	// ************************ Getter/Setter ************************
	@Id
	@GeneratedValue(generator = "favouritelist_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public HistoUser getOwner() {
		return owner;
	}

	public void setOwner(HistoUser owner) {
		this.owner = owner;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEditAble() {
		return editAble;
	}

	public void setEditAble(boolean editAble) {
		this.editAble = editAble;
	}

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public boolean isDefaultList() {
		return defaultList;
	}

	public void setDefaultList(boolean defaultList) {
		this.defaultList = defaultList;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	public List<FavouriteListItem> getItems() {
		return items;
	}

	public void setItems(List<FavouriteListItem> items) {
		this.items = items;
	}

}
