package org.histo.action;

import org.histo.action.dialog.ProgrammVersionDialog;
import org.histo.action.dialog.media.MediaDialog;
import org.histo.action.dialog.notification.ContactDialog;
import org.histo.action.dialog.notification.ContactNotificationDialog;
import org.histo.action.dialog.notification.ContactSelectDialog;
import org.histo.action.dialog.notification.NotificationDialog;
import org.histo.action.dialog.notification.NotificationPhaseExitDialog;
import org.histo.action.dialog.patient.DeleteTaskDialog;
import org.histo.action.dialog.patient.EditPatientDialog;
import org.histo.action.dialog.patient.PatientLogDialog;
import org.histo.action.dialog.print.CustomAddressDialog;
import org.histo.action.dialog.print.FaxPrintDocumentDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.dialog.settings.groups.GroupEditDialog;
import org.histo.action.dialog.settings.groups.GroupListDialog;
import org.histo.action.dialog.settings.material.MaterialEditDialog;
import org.histo.action.dialog.settings.organizations.OrganizationEditDialog;
import org.histo.action.dialog.settings.organizations.OrganizationListDialog;
import org.histo.action.dialog.settings.physician.PhysicianEditDialog;
import org.histo.action.dialog.settings.physician.PhysicianSearchDialog;
import org.histo.action.dialog.settings.staining.StainingEditDialog;
import org.histo.action.dialog.settings.users.EditUserDialog;
import org.histo.action.dialog.settings.users.UserListDialog;
import org.histo.action.dialog.slide.CreateSlidesDialog;
import org.histo.action.dialog.slide.SlideOverviewDialog;
import org.histo.action.dialog.slide.StainingPhaseExitDialog;
import org.histo.action.dialog.task.ArchiveTaskDialog;
import org.histo.action.dialog.task.ChangeMaterialDialog;
import org.histo.action.dialog.task.ChangeTaskIDDialog;
import org.histo.action.dialog.task.CreateSampleDialog;
import org.histo.action.dialog.task.CreateTaskDialog;
import org.histo.action.dialog.task.DeleteTaskEntityDialog;
import org.histo.action.dialog.worklist.WorklistSearchDialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class DialogHandlerAction {

	private OrganizationListDialog organizationListDialog;

	private WorklistSearchDialog worklistSearchDialog;

	private PrintDialog printDialog;

	private CustomAddressDialog customAddressDialog;

	private ContactSelectDialog contactSelectDialog;

	private ContactDialog contactDialog;

	private ContactNotificationDialog contactNotificationDialog;

	private ChangeMaterialDialog changeMaterialDialog;

	private NotificationDialog notificationDialog;

	private MediaDialog mediaDialog;

	private FaxPrintDocumentDialog faxPrintDocumentDialog;

	private DeleteTaskDialog deleteTaskDialog;

	private EditPatientDialog editPatientDialog;

	private SlideOverviewDialog slideOverviewDialog;

	private ArchiveTaskDialog archiveTaskDialog;

	private StainingPhaseExitDialog stainingPhaseExitDialog;

	private PhysicianSearchDialog physicianSearchDialog;

	private PhysicianEditDialog physicianEditDialog;

	private MaterialEditDialog materialEditDialog;

	private StainingEditDialog stainingEditDialog;

	private GroupListDialog groupListDialog;

	private GroupEditDialog groupEditDialog;

	private UserListDialog userListDialog;

	private PatientLogDialog patientLogDialog;

	private CreateSlidesDialog createSlidesDialog;

	private ChangeTaskIDDialog changeTaskIDDialog;

	private DeleteTaskEntityDialog deleteTaskEntityDialog;
	
	private CreateSampleDialog createSampleDialog;
	
	private ProgrammVersionDialog programmVersionDialog;
	
	private EditUserDialog editUserDialog;
	
	private OrganizationEditDialog organizationEditDialog;
	
	private NotificationPhaseExitDialog notificationPhaseExitDialog;
	
	public OrganizationListDialog getOrganizationListDialog() {
		if (organizationListDialog == null)
			organizationListDialog = new OrganizationListDialog();

		return organizationListDialog;
	}

	public WorklistSearchDialog getWorklistSearchDialog() {
		if (worklistSearchDialog == null)
			worklistSearchDialog = new WorklistSearchDialog();

		return worklistSearchDialog;
	}

	public PrintDialog getPrintDialog() {
		if (printDialog == null)
			printDialog = new PrintDialog();

		return printDialog;
	}

	public CustomAddressDialog getCustomAddressDialog() {
		if (customAddressDialog == null)
			customAddressDialog = new CustomAddressDialog();

		return customAddressDialog;
	}

	public ContactSelectDialog getContactSelectDialog() {
		if (contactSelectDialog == null)
			contactSelectDialog = new ContactSelectDialog();

		return contactSelectDialog;
	}

	public ContactDialog getContactDialog() {
		if (contactDialog == null)
			contactDialog = new ContactDialog();

		return contactDialog;
	}

	public ContactNotificationDialog getContactNotificationDialog() {
		if (contactNotificationDialog == null)
			contactNotificationDialog = new ContactNotificationDialog();

		return contactNotificationDialog;
	}

	public ChangeMaterialDialog getChangeMaterialDialog() {
		if (changeMaterialDialog == null)
			changeMaterialDialog = new ChangeMaterialDialog();

		return changeMaterialDialog;
	}

	public NotificationDialog getNotificationDialog() {
		if (notificationDialog == null)
			notificationDialog = new NotificationDialog();

		return notificationDialog;
	}

	public MediaDialog getMediaDialog() {
		if (mediaDialog == null)
			mediaDialog = new MediaDialog();

		return mediaDialog;
	}

	public FaxPrintDocumentDialog getFaxPrintDocumentDialog() {
		if (faxPrintDocumentDialog == null)
			faxPrintDocumentDialog = new FaxPrintDocumentDialog();

		return faxPrintDocumentDialog;
	}

	public DeleteTaskDialog getDeleteTaskDialog() {
		if (deleteTaskDialog == null) {
			deleteTaskDialog = new DeleteTaskDialog();
		}

		return deleteTaskDialog;
	}

	public EditPatientDialog getEditPatientDialog() {
		if (editPatientDialog == null) {
			editPatientDialog = new EditPatientDialog();
		}

		return editPatientDialog;
	}

	public SlideOverviewDialog getSlideOverviewDialog() {
		if (slideOverviewDialog == null) {
			slideOverviewDialog = new SlideOverviewDialog();
		}

		return slideOverviewDialog;
	}

	public StainingPhaseExitDialog getStainingPhaseExitDialog() {
		if (stainingPhaseExitDialog == null) {
			stainingPhaseExitDialog = new StainingPhaseExitDialog();
		}

		return stainingPhaseExitDialog;
	}

	public ArchiveTaskDialog getArchiveTaskDialog() {
		if (archiveTaskDialog == null) {
			archiveTaskDialog = new ArchiveTaskDialog();
		}

		return archiveTaskDialog;
	}

	public PhysicianSearchDialog getPhysicianSearchDialog() {
		if (physicianSearchDialog == null) {
			physicianSearchDialog = new PhysicianSearchDialog();
		}

		return physicianSearchDialog;
	}

	public PhysicianEditDialog getPhysicianEditDialog() {
		if (physicianEditDialog == null) {
			physicianEditDialog = new PhysicianEditDialog();
		}

		return physicianEditDialog;
	}

	public MaterialEditDialog getMaterialEditDialog() {
		if (materialEditDialog == null) {
			materialEditDialog = new MaterialEditDialog();
		}

		return materialEditDialog;
	}

	public StainingEditDialog getStainingEditDialog() {
		if (stainingEditDialog == null) {
			stainingEditDialog = new StainingEditDialog();
		}

		return stainingEditDialog;
	}

	public GroupListDialog getGroupListDialog() {
		if (groupListDialog == null) {
			groupListDialog = new GroupListDialog();
		}

		return groupListDialog;
	}

	public GroupEditDialog getGroupEditDialog() {
		if (groupEditDialog == null) {
			groupEditDialog = new GroupEditDialog();
		}

		return groupEditDialog;
	}

	public UserListDialog getUserListDialog() {
		if (userListDialog == null) {
			userListDialog = new UserListDialog();
		}

		return userListDialog;
	}

	public PatientLogDialog getPatientLogDialog() {
		if (patientLogDialog == null) {
			patientLogDialog = new PatientLogDialog();
		}

		return patientLogDialog;
	}

	public CreateSlidesDialog getCreateSlidesDialog() {
		if (createSlidesDialog == null) {
			createSlidesDialog = new CreateSlidesDialog();
		}

		return createSlidesDialog;
	}

	public ChangeTaskIDDialog getChangeTaskIDDialog() {
		if (changeTaskIDDialog == null) {
			changeTaskIDDialog = new ChangeTaskIDDialog();
		}

		return changeTaskIDDialog;
	}

	public DeleteTaskEntityDialog getDeleteTaskEntityDialog() {
		if (deleteTaskEntityDialog == null) {
			deleteTaskEntityDialog = new DeleteTaskEntityDialog();
		}

		return deleteTaskEntityDialog;
	}
	
	public CreateSampleDialog getCreateSampleDialog() {
		if (createSampleDialog == null) {
			createSampleDialog = new CreateSampleDialog();
		}

		return createSampleDialog;
	}
	
	public ProgrammVersionDialog getProgrammVersionDialog() {
		if (programmVersionDialog == null) {
			programmVersionDialog = new ProgrammVersionDialog();
		}

		return programmVersionDialog;
	}
	
	public EditUserDialog getEditUserDialog() {
		if (editUserDialog == null) {
			editUserDialog = new EditUserDialog();
		}

		return editUserDialog;
	}
	
	public OrganizationEditDialog getOrganizationEditDialog() {
		if (organizationEditDialog == null) {
			organizationEditDialog = new OrganizationEditDialog();
		}

		return organizationEditDialog;
	}
	
	public NotificationPhaseExitDialog getNotificationPhaseExitDialog() {
		if (notificationPhaseExitDialog == null) {
			notificationPhaseExitDialog = new NotificationPhaseExitDialog();
		}

		return notificationPhaseExitDialog;
	}
	
}
