package org.histo.action.dialog.print;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateCouncil;
import org.histo.template.documents.DiagnosisReport;
import org.histo.template.documents.TemplateUReport;
import org.histo.template.ui.documents.CouncilReportUi;
import org.histo.template.ui.documents.DocumentUi;
import org.histo.ui.LazyPDFGuiManager;
import org.histo.ui.selectors.ContactSelector;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.StreamUtils;
import org.histo.util.pdf.LazyPDFReturnHandler;
import org.histo.util.pdf.PDFGenerator;
import org.histo.util.pdf.PrintOrder;
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
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	/**
	 * Manager for rendering the pdf lazy style
	 */
	private LazyPDFGuiManager guiManager = new LazyPDFGuiManager();

	/**
	 * List of all templates for printing
	 */
	private List<DocumentUi<?>> templateList;

	/**
	 * The TemplateListtransformer for selecting a template
	 */
	private DefaultTransformer<DocumentUi<?>> templateTransformer;

	/**
	 * Ui object for template
	 */
	private DocumentUi<?> selectedTemplate;

	/**
	 * Can be set to true, if so the generated pdf will be saved
	 */
	private boolean savePDF;

	/**
	 * if true no print button, but instead a select button will be display
	 */
	private boolean selectMode;

	/**
	 * If true only on address can be selected
	 */
	private boolean singleAddressSelectMode;

	/**
	 * If true a fax button will be displayed
	 */
	private boolean faxMode;

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

		List<DocumentTemplate> subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
				DocumentType.U_REPORT, DocumentType.U_REPORT_EMTY, DocumentType.DIAGNOSIS_REPORT_EXTERN);

		// getting ui objects
		List<DocumentUi<?>> subSelectUIs = subSelect.stream().map(p -> p.getDocumentUi()).collect(Collectors.toList());
		// init templates
		subSelectUIs.forEach(p -> p.initialize(task));

		initBean(task, subSelectUIs, DocumentType.DIAGNOSIS_REPORT);

		setSelectMode(false);

		setFaxMode(true);

		setSavePDF(true);

		setSingleAddressSelectMode(false);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initAndPrepareBeanForCouncil(Task task) {
		initBeanForCouncil(task, null);
		prepareDialog();
	}

	public void initAndPrepareBeanForCouncil(Task task, Council council) {
		initBeanForCouncil(task, council);
		prepareDialog();
	}

	public void initBeanForCouncil(Task task, Council council) {
		List<DocumentTemplate> subSelect = DocumentTemplate.getTemplates(DocumentType.COUNCIL_REQUEST);

		// getting ui objects
		List<DocumentUi<?>> subSelectUIs = subSelect.stream().map(p -> p.getDocumentUi()).collect(Collectors.toList());

		// init uis
		subSelectUIs.stream().forEach(p -> ((CouncilReportUi) p).initialize(task, council));

		initBean(task, subSelectUIs, DocumentType.COUNCIL_REQUEST);

		setSelectMode(false);

		setSingleAddressSelectMode(false);

		onChangePrintTemplate();
	}

	public void initBeanForExternalDisplay(Task task, List<DocumentType> types, DocumentType defaultType) {
		initBeanForExternalDisplay(task, DocumentTemplate.getTemplates(types), defaultType, null);
	}

	public void initBeanForExternalDisplay(Task task, List<DocumentTemplate> types, DocumentType defaultType,
			AssociatedContact sendTo) {

		List<DocumentUi<?>> subSelectUIs = types.stream().map(p -> p.getDocumentUi()).collect(Collectors.toList());

		// init templates
		subSelectUIs.forEach(p -> p.initialize(task));

		initBean(task, subSelectUIs, defaultType);

		setSelectMode(false);

		setFaxMode(false);

		setSingleAddressSelectMode(false);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBeanForSelecting(Task task, DocumentType[] types, DocumentType defaultType,
			AssociatedContact[] addresses, boolean allowIndividualAddress) {
		initBeanForSelecting(task, DocumentTemplate.getTemplates(types), defaultType, Arrays.asList(addresses),
				allowIndividualAddress);
	}

	public void initBeanForSelecting(Task task, List<DocumentTemplate> types, DocumentType defaultType,
			List<AssociatedContact> addresses, boolean allowIndividualAddress) {

		List<DocumentUi<?>> subSelectUIs = types.stream().map(p -> p.getDocumentUi()).collect(Collectors.toList());

		// init templates
		subSelectUIs.forEach(p -> p.initialize(task));

		initBeanForSelecting(task, subSelectUIs, defaultType);
	}

	public void initBeanForSelecting(Task task, List<DocumentUi<?>> subSelectUIs, DocumentType defaultType) {

		subSelectUIs.forEach(p -> p.setUpdatePdfOnEverySettingChange(true));

		initBean(task, subSelectUIs, defaultType);

		setSelectMode(true);

		// rendering the template
		onChangePrintTemplate();
	}

	public void initBean(Task task, List<DocumentUi<?>> templateUI, DocumentType defaultTemplate) {

		// getting task datalist, if was altered a updated task will be returend
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.PRINT);

		if (templateUI != null) {
			setTemplateList(templateUI);

			setTemplateTransformer(new DefaultTransformer<DocumentUi<?>>(getTemplateList()));

			try {
				setSelectedTemplate(templateUI.stream()
						.filter(p -> p.getDocumentTemplate().isDefaultOfType()
								&& p.getDocumentTemplate().getDocumentType() == defaultTemplate)
						.collect(StreamUtils.singletonCollector()));
			} catch (IllegalStateException e) {
				if (!getTemplateList().isEmpty())
					setSelectedTemplate(getTemplateList().get(0));
			}

			guiManager.setRenderComponent(true);
		} else {
			guiManager.setRenderComponent(false);
		}

		guiManager.reset();
	}

	public void onChangePrintTemplate() {
		guiManager.reset();
		guiManager.startRendering(getSelectedTemplate().getDefaultTemplateConfiguration());
	}

	public void onPrintNewPdf() {

		PDFGenerator generator = new PDFGenerator();

		while (getSelectedTemplate().hasNextTemplateConfiguration()) {
			DocumentTemplate template = getSelectedTemplate().getNextTemplateConfiguration();
			PDFContainer pdf = generator.getPDF(template);

			PrintOrder printOrder = new PrintOrder(pdf, template);

			if (!template.isTransientContent()) {
			}

			userHandlerAction.getSelectedPrinter().print(printOrder);

			// contactDAO.addNotificationType(task, contactChooser.getContact(),
			// AssociatedContactNotification.NotificationTyp.PRINT, false, true, false,
			// new Date(System.currentTimeMillis()), contactChooser.getCustomAddress());
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
				genericDAO.savePatientData(pdf, task, "log.patient.task.pdf.created", pdf.getName());

				task.getAttachedPdfs().add(pdf);

				genericDAO.savePatientData(task, "log.patient.pdf.attached", pdf.getName());
			} else {
				logger.debug("PDF allready saved, not saving. " + pdf.getName());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Returns the rendered pdf if in selectMode
	 */
	public void hideAndSelectDialog() {
		RequestContext.getCurrentInstance().closeDialog(guiManager.getPDFContainerToRender());
	}
} 
