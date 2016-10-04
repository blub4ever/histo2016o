package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Contact;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ContactHandlerAction implements Serializable {

	private static final long serialVersionUID = -3672859612072175725L;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private PhysicianDAO physicianDAO;

	/**
	 * List with all available contacts
	 */
	private List<Contact> allAvailableContact;

	private boolean personSurgeon = true;

	private boolean personExtern = true;

	private boolean personOther = true;

	private boolean addedContacts = false;

	/**
	 * Gets a list with all available contact for a specific task. Filters all
	 * duplicated entries.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param addedContact
	 */
	public void prepareContacts(Task task, boolean surgeon, boolean extern, boolean other, boolean addedContact) {

		genericDAO.refresh(task);

		setAllAvailableContact(new ArrayList<Contact>());

		List<Contact> contacts = task.getContacts();

		List<Physician> databaseContacts = physicianDAO.getPhysicians(surgeon, extern, other);

		if (!addedContact) {
			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					if (contact.getPhysician().getId() == physician.getId()) {
						contact.setSelected(true);
						getAllAvailableContact().add(contact);
						System.out.println("found continue");
						continue loop;
					}
				}

				getAllAvailableContact().add(new Contact(physician));

			}
			// Nur bereits verwendete Kontakte anzeigen
		} else {
			getAllAvailableContact().addAll(contacts);
		}

		helper.showDialog(HistoSettings.DIALOG_WORKLIST_CONTACTS_ADD, 1024, 600, false, false, true);
	}

	/**
	 * Aktualisiert die Liste der vorhanden Kontakte.
	 * 
	 * @param contacts
	 * @param task
	 */
	public void updateContactList(List<Contact> contacts, Task task) {
		for (Contact contact : contacts) {
			if (contact.isSelected()) {
				if (contact.getRole() == 0) {
					task.getContacts().remove(contact);
				}
				continue;
			} else if (contact.getRole() != 0)
				task.getContacts().add(contact);
		}

		genericDAO.save(task);
		hideContactsDialog();
	}

	/**
	 * Sobald im Kontaktdialog ein neuer Kontakt ausgew�hlt wird, wird je nach
	 * Art eine Benachrichtigung vorausgew�hlt.
	 * 
	 * @param contact
	 */
	public void onContactChangeRole(Contact contact) {
		// contact wurde deselektiert alles auf nicht benutzt setzten
		if (contact.getRole() == Contact.ROLE_NONE) {
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
		} else {
			// es wurde schon etwas ausgew�hlt, alles so belassen wie es war
			if (contact.isUseEmail() || contact.isUsePhone() || contact.isUseFax())
				return;

			// bei internen operateuren mail bevorzugen
			if (contact.getRole() == Contact.ROLE_SURGEON) {
				contact.setUseEmail(true);
				return;
			}

			// bei externen die eine Faxnummer haben fax bevorzugen
			if (contact.getRole() == Contact.ROLE_EXTERN && contact.getPhysician().getFax() != null
					&& !contact.getPhysician().getFax().isEmpty()) {
				contact.setUseFax(true);
				return;
			}
			// in allen anderen f�llen email setzten
			if (contact.getPhysician().getEmail() != null && !contact.getPhysician().getEmail().isEmpty())
				contact.setUseEmail(true);
		}
	}

	public void sendTest() {
		
		FileUtil.loadTextFile(null);
		
		
		
//		System.out.println("ok");
//		SimpleEmail email = new SimpleEmail();
//		email.setHostName("smtp.ukl.uni-freiburg.de");
//		email.setDebug(true);
//		email.setSmtpPort(465);
//		email.setSSLOnConnect(true);
//		try {
//			email.addTo("andreas.glatz@uniklinik-freiburg.de");
//			email.setFrom("augenklinik.histologie@uniklinik-freiburg.de", "Name des Senders");
//			email.setSubject("Testnachricht");
//			email.setMsg("Hallo, das ist nur ein simpler Test");
//			email.send();
//		} catch (EmailException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

	/**
	 * Schlie�t den Kontakt Dialog
	 */
	public void hideContactsDialog() {
		helper.hideDialog(HistoSettings.DIALOG_WORKLIST_CONTACTS_ADD);
	}

	public void preparePerformContactsDialog() {
		helper.showDialog(HistoSettings.DIALOG_WORKLIST_CONTACTS_PERFORM, 1024, 600, false, false, true);
	}

	public void hidePerformContactsDialog() {
		helper.hideDialog(HistoSettings.DIALOG_WORKLIST_CONTACTS_PERFORM);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public List<Contact> getAllAvailableContact() {
		return allAvailableContact;
	}

	public void setAllAvailableContact(List<Contact> allAvailableContact) {
		this.allAvailableContact = allAvailableContact;
	}

	public boolean isPersonSurgeon() {
		return personSurgeon;
	}

	public void setPersonSurgeon(boolean personSurgeon) {
		this.personSurgeon = personSurgeon;
	}

	public boolean isPersonExtern() {
		return personExtern;
	}

	public void setPersonExtern(boolean personExtern) {
		this.personExtern = personExtern;
	}

	public boolean isPersonOther() {
		return personOther;
	}

	public void setPersonOther(boolean personOther) {
		this.personOther = personOther;
	}

	public boolean isAddedContacts() {
		return addedContacts;
	}

	public void setAddedContacts(boolean addedContacts) {
		this.addedContacts = addedContacts;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
