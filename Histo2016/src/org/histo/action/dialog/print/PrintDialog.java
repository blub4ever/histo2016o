package org.histo.action.dialog.print;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.ui.ContactContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.printer.PrintTemplate;
import org.primefaces.context.RequestContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PrintDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	/**
	 * List of all templates for printing
	 */
	private List<PrintTemplate> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<PrintTemplate> templateTransformer;

	/**
	 * Selected template for printing
	 */
	private PrintTemplate selectedTemplate;

	/**
	 * Generated or loaded PDf
	 */
	private PDFContainer pdfContainer;

	/**
	 * List with all associated contacts
	 */
	private List<ContactContainer> contactList;

	/**
	 * The associatedContact rendered, the first one will always be rendered, if
	 * not changed, no rendering necessary
	 */
	private ContactContainer selectedContact;

	/**
	 * True if the pdf should be rendered
	 */
	private boolean renderPdf;

	/**
	 * Council to print
	 */
	private Council selectedCouncil;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBeanForPrinting(Task task) {
		initBeanForPrinting(task);
		prepareDialog();
	}

	public void initBeanForPrinting(Task task) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.U_REPORT,
						DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		// contacts for printing
		setContactList(new ArrayList<ContactContainer>());

		// setting patient
		getContactList().add(new ContactContainer(task, task.getPatient().getPerson(), ContactRole.PATIENT));

		// setting other contacts (physicians)
		getContactList().addAll(ContactContainer.factory(task.getContacts()));

		setSelectedContact(null);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initAndPrepareBeanForCouncil(Task task, Council council) {
		initBeanForCouncil(task, council);
		prepareDialog();
	}

	public void initBeanForCouncil(Task task, Council council) {
		PrintTemplate[] subSelect = PrintTemplate
				.getTemplatesByTypes(new DocumentType[] { DocumentType.COUNCIL_REQUEST });

		initBean(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect));

		setSelectedCouncil(council);

		// contacts for printing
		setContactList(new ArrayList<ContactContainer>());

		// only one adress so set as chosen
		if (getSelectedCouncil().getCouncilPhysician() != null) {
			ContactContainer chosser = new ContactContainer(task,
					getSelectedCouncil().getCouncilPhysician().getPerson(), ContactRole.CASE_CONFERENCE);
			chosser.setSelected(true);
			// setting patient
			getContactList().add(chosser);

			// setting council physicians data as rendere associatedContact data
			setSelectedContact(chosser);
		}

		onChangePrintTemplate();
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType) {
		initBeanForExternalDisplay(task, types, defaultType, null);
	}

	public void initBeanForExternalDisplay(Task task, DocumentType[] types, DocumentType defaultType,
			AssociatedContact sendTo) {
		PrintTemplate[] subSelect = PrintTemplate.getTemplatesByTypes(types);
		initBeanForExternalDisplay(task, subSelect, PrintTemplate.getDefaultTemplate(subSelect, defaultType), sendTo);
	}

	public void initBeanForExternalDisplay(Task task, PrintTemplate[] types, PrintTemplate defaultType,
			AssociatedContact sendTo) {

		initBean(task, types, defaultType);

		setContactList(new ArrayList<ContactContainer>());

		setSelectedContact(new ContactContainer(sendTo));

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBean(Task task, PrintTemplate[] templates, PrintTemplate selectedTemplate) {
		// getting task datalist, if was altered a updated task will be returend
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.PRINT);

		if (templates != null) {
			setTemplateList(new ArrayList<PrintTemplate>(Arrays.asList(templates)));

			setTemplateTransformer(new DefaultTransformer<PrintTemplate>(getTemplateList()));

			// sets the selected template
			if (selectedTemplate == null && !getTemplateList().isEmpty())
				setSelectedTemplate(getTemplateList().get(0));
			else
				setSelectedTemplate(selectedTemplate);
		}
	}

	/**
	 * Updates the pdf content if a associatedContact was chosen for the first
	 * time
	 */
	public void onChooseContact(ContactContainer container) {

		//
		// if (getSelectedContact() == null && !selectedContacts.isEmpty()) {
		// setSelectedContact(selectedContacts.get(0).getContact());
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// } else if (getSelectedContact() != null) {
		// boolean found = false;
		// for (ContactContainer contactChooser : selectedContacts) {
		// if (contactChooser.getContact() == getSelectedContact()) {
		// found = true;
		// break;
		// }
		// }
		//
		// if (!found) {
		// if (!selectedContacts.isEmpty()) {
		// setSelectedContact(selectedContacts.get(0).getContact());
		// } else
		// setSelectedContact(null);
		// onChangePrintTemplate();
		// RequestContext.getCurrentInstance().update("dialogContent");
		// }
		// }
	}

	public void onChooseOrganizationOfContact(ContactContainer.OrganizationChooser chooser) {

		// only one organization can be selected, removing other organizations
		// from selection
		if (chooser.getParent().isSelected()) {
			for (ContactContainer.OrganizationChooser organizationChooser : chooser.getParent()
					.getOrganizazionsChoosers()) {
				if (organizationChooser != chooser)
					organizationChooser.setSelected(false);
			}
		} else {
			// setting parent as selected
			chooser.getParent().setSelected(true);
		}
	}

	public void onChangePrintTemplate() {
		setPdfContainer(generatePDFFromTemplate());
		setRenderPdf(getPdfContainer() == null ? false : true);
	}

	private PDFContainer generatePDFFromTemplate() {
		PDFContainer result;
		switch (getSelectedTemplate().getDocumentTyp()) {
		case U_REPORT:
		case U_REPORT_EMTY:
			result = pDFGeneratorHandler.generateUReport(getSelectedTemplate(), getTask().getPatient(), getTask());
			break;
		case DIAGNOSIS_REPORT:
			result = pDFGeneratorHandler.generateDiagnosisReport(getSelectedTemplate(), getTask().getPatient(),
					getTask(),
					getSelectedContact() == null ? resourceBundle.get("pdf.address.none") : getSelectedContact());
			break;
		case COUNCIL_REQUEST:
			result = pDFGeneratorHandler.generateCouncilRequest(getSelectedTemplate(), getTask().getPatient(),
					getSelectedCouncil());
			break;
		default:
			// always render the pdf with the fist associatedContact chosen
			result = pDFGeneratorHandler.generatePDFForReport(getTask().getPatient(), getTask(), getSelectedTemplate(),
					getSelectedContact() == null ? null : getSelectedContact());
			break;
		}

		if (result == null) {
			result = new PDFContainer(DocumentType.EMPTY, "", new byte[0]);
			logger.debug("No Pdf created, hiding pdf display");
		}
		return result;
	}

	/**
	 * Gets the selected contacts an returns an list including them
	 * 
	 * @return
	 */
	private List<ContactContainer> getSelectedContactFromList() {
		ArrayList<ContactContainer> result = new ArrayList<ContactContainer>();
		for (ContactContainer contactChooser : getContactList()) {
			if (contactChooser.isSelected())
				result.add(contactChooser);
		}

		logger.debug("Return " + result.size() + " selected contatcs");
		return result;
	}

	/**
	 * Return the pdf as streamed content
	 * 
	 * @return
	 */
	public StreamedContent getPdfContent() {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream(getPdfContainer().getData()), "application/pdf",
					getPdfContainer().getName());
		}
	}

	public void onDownloadPdf() {
		if (getPdfContainer().getId() == 0) {
			logger.debug("Pdf not saved jet, saving");
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
		}
	}

	public void onPrintNewPdf() {
		// no address was chosen, so the address will be "An den
		// weiterbehandelden Kollegen" this was generated and saved in
		// tmpPdfContainer
		if (getContactList().isEmpty()) {
			if (!getSelectedTemplate().isDoNotSave())
				savePdf(getTask(), getPdfContainer());
			settingsHandler.getSelectedPrinter().print(getPdfContainer());
		} else {
			boolean oneContactSelected = false;
			// addresses where chosen
			for (ContactContainer contactChooser : getContactList()) {
				if (contactChooser.isSelected()) {
					// address of the rendered pdf, not rendering twice
					if (contactChooser.getContact() == getSelectedContact()) {
						if (!getSelectedTemplate().isDoNotSave())
							savePdf(getTask(), getPdfContainer());
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(getPdfContainer());
						}
					} else {
						// setting other associatedContact then selected
						AssociatedContact tmp = getSelectedContact();
						setSelectedContact(contactChooser.getContact());
						// render all other pdfs
						PDFContainer otherAddress = generatePDFFromTemplate();
						for (int i = 0; i < contactChooser.getCopies(); i++) {
							settingsHandler.getSelectedPrinter().print(otherAddress);
						}
						// settings the old selected associatedContact as
						// selected associatedContact
						setSelectedContact(tmp);
					}

					oneContactSelected = true;
				}

			}

			// printin if no container was selected, with the default address
			if (!oneContactSelected) {
				settingsHandler.getSelectedPrinter().print(getPdfContainer());
			}
		}

	}

	/**
	 * Saves a new pdf within the task
	 * 
	 * @param pdf
	 */
	public void savePdf(Task task, PDFContainer pdf) {

		try {
			if (pdf.getId() == 0) {
				logger.debug("Pdf not saved jet, saving" + pdf.getName());

				// saving new pdf and updating task
				patientDao.savePatientAssociatedDataFailSave(pdf, task, "log.patient.task.pdf.created", pdf.getName());

				task.getAttachedPdfs().add(pdf);

				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.pdf.attached", pdf.getName());
			} else {
				logger.debug("PDF allready saved, not saving. " + pdf.getName());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
