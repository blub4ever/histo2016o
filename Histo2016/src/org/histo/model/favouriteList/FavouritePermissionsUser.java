package org.histo.model.favouriteList;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.model.user.HistoUser;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@Getter
@Setter
public class FavouritePermissionsUser extends FavouritePermissions {

	@OneToOne
	private HistoUser user;

	public FavouritePermissionsUser() {
	}

	public FavouritePermissionsUser(HistoUser user) {
		this.user = user;
	}
}
