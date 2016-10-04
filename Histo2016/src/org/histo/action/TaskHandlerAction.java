package org.histo.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Display;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.PDFContainer;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.model.util.transientObjects.PDFTemplate;
import org.histo.ui.transformer.PdfTemplateTransformer;
import org.histo.ui.transformer.StainingListTransformer;
import org.histo.util.FileUtil;
import org.histo.util.PdfUtil;
import org.histo.util.ResourceBundle;
import org.histo.util.TaskUtil;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskHandlerAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1460063099758733063L;

	@Autowired
	HelperHandlerAction helper;

	@Autowired
	HelperDAO helperDAO;

	@Autowired
	TaskDAO taskDAO;

	@Autowired
	GenericDAO genericDAO;

	@Autowired
	DiagnosisHandlerAction diagnosisHandlerAction;

	@Autowired
	SlideHandlerAction slideHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	private Task taskToPrint;

	private List<PDFTemplate> templates;

	private PDFTemplate selectedTemplate;

	private PdfTemplateTransformer templateTransformer;

	private StreamedContent pdfContent;

	private String printer;

	private int copies;

	/********************************************************
	 * Task creation
	 ********************************************************/
	/**
	 * all staininglists, default not initialized
	 */
	private List<MaterialPreset> allAvailableMaterials;

	/**
	 * selected stainingList for task
	 */
	private MaterialPreset selectedMaterial;

	/**
	 * Transformer for selecting staininglist
	 */
	private StainingListTransformer materialListTransformer;

	/**
	 * Temporary task for creating samples
	 */
	private Task temporaryTask;

	/********************************************************
	 * Task creation
	 ********************************************************/

	private HashMap<String, String> selectableWards;

	public void prepareForTask() {
		setAllAvailableMaterials(helperDAO.getAllStainingLists());
		setMaterialListTransformer(new StainingListTransformer(getAllAvailableMaterials()));

		if (selectableWards == null) {
			selectableWards = new HashMap<String, String>();
			selectableWards.put("none", resourceBundle.get("#{msg['body.receiptlog.ward.select']}"));
			selectableWards.put("ambulant", resourceBundle.get("#{msg['body.receiptlog.ward.ambulant']}"));
			selectableWards.put("impatient", resourceBundle.get("#{msg['body.receiptlog.ward.inpatient']}"));
			selectableWards.put("private", resourceBundle.get("#{msg['body.receiptlog.ward.ambulant.private']}"));
			selectableWards.put("impatient-private",
					resourceBundle.get("#{msg['body.receiptlog.ward.inpatient.private']}"));
			selectableWards.put("ims", resourceBundle.get("#{msg['body.receiptlog.ward.ims']}"));
			selectableWards.put("extern", resourceBundle.get("#{msg['body.receiptlog.ward.extern']}"));
			selectableWards.put("extern-private", resourceBundle.get("#{msg['body.receiptlog.ward.exern.private']}"));
			selectableWards.put("fda", resourceBundle.get("#{msg['body.receiptlog.ward.fda']}"));
			selectableWards.put("manz", resourceBundle.get("#{msg['body.receiptlog.ward.manz']}"));
			selectableWards.put("beck", resourceBundle.get("#{msg['body.receiptlog.ward.beck']}"));
			selectableWards.put("axenfeld", resourceBundle.get("#{msg['body.receiptlog.ward.axenfeld']}"));
		}
	}

	/**
	 * Displays a dialog for creating a new task
	 */
	public void prepareNewTaskDialog() {
		prepareForTask();

		// checks if default statingsList is empty
		if (!getAllAvailableMaterials().isEmpty()) {
			setSelectedMaterial(getAllAvailableMaterials().get(0));
		}

		helper.showDialog(HistoSettings.DIALOG_CREATE_TASK, false, false, true);
	}

	/**
	 * Hides the dialog for creating new tasks
	 */
	public void hideNewTaskDialog() {
		helper.hideDialog(HistoSettings.DIALOG_CREATE_TASK);
	}

	/**
	 * Creates a new task for the given Patient
	 * 
	 * @param patient
	 */
	public void createNewTask(Patient patient, MaterialPreset material) {
		if (patient.getTasks() == null) {
			patient.setTasks(new ArrayList<>());
		}

		Task task = TaskUtil.createNewTask(patient, taskDAO.countSamplesOfCurrentYear());

		patient.getTasks().add(0, task);
		// sets the new task as the selected task
		patient.setSelectedTask(task);

		genericDAO.save(task, resourceBundle.get("log.patient.task.new"), patient);

		createNewSample(task, material);

		genericDAO.save(patient, resourceBundle.get("log.patient.save"), patient);

		hideNewTaskDialog();
	}

	/**
	 * Displays a dialog for creating a new sample
	 */
	public void prepareNewSampleDialog(Task task) {
		setAllAvailableMaterials(helperDAO.getAllStainingLists());
		// checks if default statingsList is empty
		if (!getAllAvailableMaterials().isEmpty()) {
			setSelectedMaterial(getAllAvailableMaterials().get(0));
			setMaterialListTransformer(new StainingListTransformer(getAllAvailableMaterials()));
		}

		setTemporaryTask(task);

		helper.showDialog(HistoSettings.DIALOG_CREATE_SAMPLE, false, false, true);
	}

	/**
	 * Hides the dialog for creating new samples
	 */
	public void hideNewSampleDialog() {
		setTemporaryTask(null);
		helper.hideDialog(HistoSettings.DIALOG_CREATE_SAMPLE);
	}

	/**
	 * Method used by the gui for creating a new sample
	 * 
	 * @param task
	 * @param material
	 */
	public void createNewSampleForGui(Task task, MaterialPreset material) {
		createNewSample(task, material);
		hideNewSampleDialog();
	}

	/**
	 * Creates a new sample and adds this sample to the given task. Creates a
	 * new diagnosis and a new block with slides as well.
	 * 
	 * @param task
	 */
	public void createNewSample(Task task, MaterialPreset material) {
		Sample sample = TaskUtil.createNewSample(task);

		sample.setMaterilaPreset(material);
		sample.setMaterial(material.getName());

		genericDAO.save(sample, resourceBundle.get("log.patient.sample.new"), task.getPatient());

		// creating first default diagnosis
		diagnosisHandlerAction.createDiagnosis(sample, Diagnosis.TYPE_DIAGNOSIS);

		// creating needed blocks
		createNewBlock(sample);

		genericDAO.save(sample);
	}

	/**
	 * Creates a new block for the given sample. Adds all slides from the
	 * material preset to the block.
	 * 
	 * @param sample
	 * @param material
	 */
	public void createNewBlock(Sample sample) {
		Block block = TaskUtil.createNewBlock(sample);

		genericDAO.save(block, resourceBundle.get("log.patient.block.new"), sample.getPatient());

		for (StainingPrototype proto : sample.getMaterilaPreset().getStainingPrototypes()) {
			slideHandlerAction.addStaining(proto, block);
		}

		genericDAO.save(block);

		sample.getParent().generateStainingGuiList();
	}

	public void preparePrintDialog(Task task) {
		setTaskToPrint(task);
		helper.showDialog(HistoSettings.DIALOG_PRINT, 1024, 600, false, false, true);
		String templateFile = FileUtil.loadTextFile(HistoSettings.PDF_TEMPLATE_JSON);
		setTemplates(Arrays.asList(PDFTemplate.factroy(templateFile)));
		setSelectedTemplate(PdfUtil.getDefaultTemplate(getTemplates()));
		setPdfContent(generatePDF(task, getSelectedTemplate()));
		setTemplateTransformer(new PdfTemplateTransformer(getTemplates()));
	}

	public void hidePrintDialog() {
		setTaskToPrint(null);
		helper.hideDialog(HistoSettings.DIALOG_PRINT);
	}

	public void changeTemplate(Task task, PDFTemplate template) {
		setPdfContent(generatePDF(task, template));
		System.out.println("chagne task");
	}

	public StreamedContent generatePDF(Task task, PDFTemplate template) {
		FacesContext context = FacesContext.getCurrentInstance();
		if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			// So, we're rendering the HTML. Return a stub StreamedContent so
			// that it will generate right URL.
			return new DefaultStreamedContent();
		} else {
			taskDAO.initializeTask(task);

			if (task.getPdfs() != null && !task.getPdfs().isEmpty()) {
				for (PDFContainer pdf : task.getPdfs()) {
					if (pdf.getType().equals(template.getType()))
						return new DefaultStreamedContent(new ByteArrayInputStream(pdf.getData()), "application/pdf");
				}
			}

			PDFContainer container = PdfUtil.createPDFContainer(template,
					PdfUtil.populatePdf(FileUtil.loadPDFFile(template.getFileWithLogo()), task));

			task.getPdfs().add(container);
			genericDAO.save(task);

			return new DefaultStreamedContent(new ByteArrayInputStream(container.getData()), "application/pdf");

		}
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public Task getTaskToPrint() {
		return taskToPrint;
	}

	public void setTaskToPrint(Task taskToPrint) {
		this.taskToPrint = taskToPrint;
	}

	public List<PDFTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(List<PDFTemplate> templates) {
		this.templates = templates;
	}

	public PDFTemplate getSelectedTemplate() {
		return selectedTemplate;
	}

	public void setSelectedTemplate(PDFTemplate selectedTemplate) {
		this.selectedTemplate = selectedTemplate;
	}

	public StreamedContent getPdfContent() {
		return pdfContent;
	}

	public void setPdfContent(StreamedContent pdfContent) {
		this.pdfContent = pdfContent;
	}

	public PdfTemplateTransformer getTemplateTransformer() {
		return templateTransformer;
	}

	public void setTemplateTransformer(PdfTemplateTransformer templateTransformer) {
		this.templateTransformer = templateTransformer;
	}

	public String getPrinter() {
		return printer;
	}

	public void setPrinter(String printer) {
		this.printer = printer;
	}

	public int getCopies() {
		return copies;
	}

	public void setCopies(int copies) {
		this.copies = copies;
	}

	public List<MaterialPreset> getAllAvailableMaterials() {
		return allAvailableMaterials;
	}

	public void setAllAvailableMaterials(List<MaterialPreset> allAvailableMaterials) {
		this.allAvailableMaterials = allAvailableMaterials;
	}

	public MaterialPreset getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setSelectedMaterial(MaterialPreset selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
	}

	public StainingListTransformer getMaterialListTransformer() {
		return materialListTransformer;
	}

	public void setMaterialListTransformer(StainingListTransformer materialListTransformer) {
		this.materialListTransformer = materialListTransformer;
	}

	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public HashMap<String, String> getSelectableWards() {
		return selectableWards;
	}

	public void setSelectableWards(HashMap<String, String> selectableWards) {
		this.selectableWards = selectableWards;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
// p:importEnum type="javax.faces.application.ProjectStage"
// var="JsfProjectStages" allSuffix="ALL_ENUM_VALUES" />