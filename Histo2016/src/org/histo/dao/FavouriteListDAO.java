package org.histo.dao;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Physician;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouriteListItem;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoUser;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope("session")
public class FavouriteListDAO extends AbstractDAO {

	private static final long serialVersionUID = 7314048344901513342L;

	@Autowired
	private GenericDAO genericDAO;

	public FavouriteList getFavouriteList(long id, boolean initialized, boolean permissions) {
		FavouriteList favList = genericDAO.get(FavouriteList.class, id);

		if (initialized) {
			Hibernate.initialize(favList.getOwner());
			Hibernate.initialize(favList.getItems());
		}

		if (permissions) {
			Hibernate.initialize(favList.getUsers());
			Hibernate.initialize(favList.getGroups());
		}

		return favList;
	}

	public FavouriteList initFavouriteList(FavouriteList favList, boolean permissions) {
		genericDAO.reattach(favList);
		Hibernate.initialize(favList.getOwner());
		Hibernate.initialize(favList.getItems());

		if (permissions) {
			Hibernate.initialize(favList.getUsers());
			Hibernate.initialize(favList.getGroups());
		}

		return favList;
	}

	public List<FavouriteList> getFavouriteListsForUser(HistoUser user) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<FavouriteList> criteria = qb.createQuery(FavouriteList.class);
		Root<FavouriteList> root = criteria.from(FavouriteList.class);
		criteria.select(root);

		Join<FavouriteList, FavouritePermissionsUser> userQuery = root.join("users", JoinType.LEFT);
		Join<FavouriteList, FavouritePermissionsGroup> groupQuery = root.join("groups", JoinType.LEFT);

		Predicate orClause = qb.or(qb.equal(root.get("owner"), user), qb.equal(root.get("globalView"), true),
				qb.equal(userQuery.get("user"), user.getId()),
				qb.equal(groupQuery.get("group"), user.getGroup().getId()));

		criteria.where(orClause);

		criteria.distinct(true);

		List<FavouriteList> favouriteLists = getSession().createQuery(criteria).getResultList();

		return favouriteLists;
	}

	public List<FavouriteList> getFavouriteListsWithTasksForUser(HistoUser user, boolean writeable, boolean readable) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<FavouriteList> criteria = qb.createQuery(FavouriteList.class);
		Root<FavouriteList> root = criteria.from(FavouriteList.class);
		criteria.select(root);

		Join<FavouriteList, FavouritePermissionsUser> userQuery = root.join("users", JoinType.LEFT);
		Join<FavouriteList, FavouritePermissionsGroup> groupQuery = root.join("groups", JoinType.LEFT);

		root.fetch("items", JoinType.LEFT);

		Predicate andUser = null;
		Predicate andGroup = null;

		if (writeable && readable) {
			andUser = qb.and(qb.equal(userQuery.get("readable"), true), qb.equal(userQuery.get("editable"), true),
					qb.equal(userQuery.get("user"), user.getId()));
			andGroup = qb.and(qb.equal(groupQuery.get("readable"), true), qb.equal(groupQuery.get("editable"), true),
					qb.equal(groupQuery.get("group"), user.getGroup().getId()));
		} else if (writeable) {
			andUser = qb.and(qb.equal(userQuery.get("editable"), true), qb.equal(userQuery.get("user"), user.getId()));
			andGroup = qb.and(qb.equal(groupQuery.get("editable"), true),
					qb.equal(groupQuery.get("group"), user.getGroup().getId()));
		} else if (readable) {
			andUser = qb.and(qb.equal(userQuery.get("readable"), true), qb.equal(userQuery.get("user"), user.getId()));
			andGroup = qb.and(qb.equal(groupQuery.get("readable"), true),
					qb.equal(groupQuery.get("group"), user.getGroup().getId()));
		} else {
			andUser = qb.equal(userQuery.get("user"), user.getId());
			andGroup = qb.equal(groupQuery.get("group"), user.getGroup().getId());
		}

		Predicate orClause = qb.or(qb.equal(root.get("owner"), user), qb.equal(root.get("globalView"), true), andUser,
				andGroup);

		criteria.where(orClause);

		criteria.distinct(true);

		List<FavouriteList> favouriteLists = getSession().createQuery(criteria).getResultList();

		return favouriteLists;
	}

	@SuppressWarnings("unchecked")
	public List<FavouriteList> getAllFavouriteLists() {
		DetachedCriteria query = DetachedCriteria.forClass(FavouriteList.class, "favList");
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

		return (List<FavouriteList>) query.getExecutableCriteria(getSession()).list();
	}

	public List<FavouriteList> getAllFavouriteLists(List<Integer> ids) {
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<FavouriteList> criteria = qb.createQuery(FavouriteList.class);
		Root<FavouriteList> root = criteria.from(FavouriteList.class);
		criteria.select(root);

		Expression<String> exp = root.get("id");
		Predicate predicate = exp.in(ids);
		criteria.where(predicate);

		List<FavouriteList> physician = getSession().createQuery(criteria).getResultList();

		return physician;
	}

	public void addTaskToList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		addTaskToList(task, predefinedFavouriteList, true);
	}

	public void addTaskToList(Task task, PredefinedFavouriteList predefinedFavouriteList, boolean refresh)
			throws CustomDatabaseInconsistentVersionException {

		reattach(task);
		reattach(task.getParent());

		addTaskToList(task, getFavouriteList(predefinedFavouriteList.getId(), true, false));
	}
	
	public void addTaskToList(Task task, long id)
			throws CustomDatabaseInconsistentVersionException {

		reattach(task);
		reattach(task.getParent());

		addTaskToList(task, getFavouriteList(id, true, false));
	}

	public void addTaskToList(Task task, FavouriteList favouriteList)
			throws CustomDatabaseInconsistentVersionException {

		// list should not contain the task
		if (favouriteList.getItems().stream().noneMatch(p -> p.getTask().getId() == task.getId())) {
			logger.debug("Adding task (" + task.getTaskID() + ") to favourite lists (" + favouriteList.getName() + ")");
			FavouriteListItem favItem = new FavouriteListItem(favouriteList, task);
			// saving new fav item
			save(favItem);
			favouriteList.getItems().add(favItem);
			// saving favlist
			save(favouriteList);
		} else {
			logger.debug("List (" + favouriteList.getName() + ") already contains task (" + task.getTaskID() + ")");
		}

		// adding to task if task is not member of this list
		if (task.getFavouriteLists().stream().noneMatch(p -> p.getId() == favouriteList.getId())) {

			logger.debug("Adding favourite list(" + favouriteList.getName() + ") to task (" + task.getTaskID() + ")");

			task.getFavouriteLists().add(favouriteList);
			genericDAO.savePatientData(task, "log.patient.task.favouriteList.added",
					new Object[] { task.getTaskID().toString(), favouriteList.toString() });

			task.generateTaskStatus();
		} else
			logger.debug("Task (" + task.getTaskID() + ") alread contains list (" + favouriteList.getName() + ")");
	}

	public void removeTaskFromList(Task task, PredefinedFavouriteList[] predefinedFavouriteLists)
			throws CustomDatabaseInconsistentVersionException {
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			removeTaskFromList(task, predefinedFavouriteList);
		}

	}

	public void removeTaskFromList(Task task, PredefinedFavouriteList predefinedFavouriteList)
			throws CustomDatabaseInconsistentVersionException {
		if (task.isListedInFavouriteList(predefinedFavouriteList)) {
			reattach(task);
			removeTaskFromList(task, getFavouriteList(predefinedFavouriteList.getId(), true, false));
		}
	}

	public void removeTaskFromList(Task task, FavouriteList favouriteList)
			throws CustomDatabaseInconsistentVersionException {
		try {
			logger.debug(
					"Removing task (" + task.getTaskID() + ") from favourite lists (" + favouriteList.getName() + ")");

			// searching for item to remove
			FavouriteListItem itemToRemove = favouriteList.getItems().stream()
					.filter(p -> p.getTask().getId() == task.getId()).collect(StreamUtils.singletonCollector());

			favouriteList.getItems().remove(itemToRemove);
			// saving new fav item
			save(favouriteList);
			genericDAO.deletePatientData(itemToRemove, task.getPatient(), "log.patient.task.favouriteList.removed",
					task.getTaskID().toString(), favouriteList.toString());
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove task (" + task.getTaskID() + ") from favourite list ("
					+ favouriteList.getName() + "), not in list");
		}

		try {
			logger.debug(
					"Removing favourite list(" + favouriteList.getName() + ") from task (" + task.getTaskID() + ")");

			FavouriteList listToRemove = task.getFavouriteLists().stream()
					.filter(p -> p.getId() == favouriteList.getId()).collect(StreamUtils.singletonCollector());

			task.getFavouriteLists().remove(listToRemove);

			// saving new fav item
			save(task);
			task.generateTaskStatus();
		} catch (IllegalStateException e) {
			// no item found
			logger.debug("Can not remove favourite list(" + favouriteList.getName() + ") from task (" + task.getTaskID()
					+ "), not listed ");
		}
		// TODO Delete FavouriteListItem?
	}

	public void removeTaskFromAllLists(Task task) {
		// removing from favouriteLists
		while (task.getFavouriteLists().size() > 0) {
			removeTaskFromList(task, getFavouriteList(task.getFavouriteLists().get(0).getId(), true, false));
		}
	}
}
