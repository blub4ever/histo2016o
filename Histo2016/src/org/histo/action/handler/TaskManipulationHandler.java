package org.histo.action.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class TaskManipulationHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	/********************************************************
	 * Diagnosis Info
	 ********************************************************/
	/**
	 * Updates all diagnosisRevision of the given diagnosisContainer
	 * 
	 * @param diagnosisContainer
	 * @param samples
	 */
	public void updateDiagnosisContainerToSampleCount(DiagnosisContainer diagnosisContainer, List<Sample> samples) {
		logger.info("Updating diagnosis info to new sample list");
		for (DiagnosisRevision revision : diagnosisContainer.getDiagnosisRevisions()) {
			updateDiagnosisRevisionToSampleCount(revision, samples);
		}

		genericDAO.savePatientData(diagnosisContainer, "log.patient.task.diagnosisContainer.update",
				diagnosisContainer);

	}

	/**
	 * Finalize all diagnosis revisions
	 * 
	 * @param revisions
	 * @param finalize
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void finalizeAllDiangosisRevisions(List<DiagnosisRevision> revisions, boolean finalize)
			throws CustomDatabaseInconsistentVersionException {
		for (DiagnosisRevision revision : revisions) {
			revision.setDiagnosisCompleted(finalize);
			revision.setCompleationDate(finalize ? System.currentTimeMillis() : 0);
			genericDAO.savePatientData(revision, finalize ? "log.patient.task.diagnosisContainer.diagnosisRevision.lock"
					: "log.patient.task.diagnosisContainer.diagnosisRevision.unlock", revision.getName());
		}
	}

	/********************************************************
	 * Diagnosis Info
	 ********************************************************/

	/********************************************************
	 * Diagnosis
	 ********************************************************/
	/**
	 * Creates a new diagnosis and adds it to the given diagnosisRevision
	 * 
	 * @param revision
	 * @param sample
	 * @return
	 */
	public Diagnosis createDiagnosis(DiagnosisRevision revision, Sample sample) {
		logger.info("Creating new diagnosis");

		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setSample(sample);
		diagnosis.setParent(revision);
		// diagnosis.setName(getDiagnosisName(sample, diagnosis,
		// resourceBundle));

		revision.getDiagnoses().add(diagnosis);

		genericDAO.savePatientData(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.new",
				diagnosis.toString());

		return diagnosis;
	}

	/**
	 * Removes a diagnosis from the parent and deletes it.
	 * 
	 * @param diagnosis
	 * @return
	 */
	public Diagnosis removeDiagnosis(Diagnosis diagnosis) {
		logger.info("Removing diagnosis " + diagnosis.getName());

		diagnosis.setSample(null);

		diagnosis.getParent().getDiagnoses().remove(diagnosis);

		genericDAO.deletePatientData(diagnosis, "log.patient.task.diagnosisContainer.diagnosis.remov", diagnosis.toString());

		return diagnosis;
	}

	/********************************************************
	 * Diagnosis
	 ********************************************************/

	/********************************************************
	 * Diagnosis Revision
	 ********************************************************/
	/**
	 * Creates a diagnosisRevision, adds it to the given DiagnosisContainer and
	 * creates also all needed diagnoses
	 * 
	 * @param parent
	 * @param type
	 * @return
	 */
	public DiagnosisRevision createDiagnosisRevision(DiagnosisContainer parent, DiagnosisRevisionType type) {
		logger.info("Creating new diagnosisRevision");

		DiagnosisRevision diagnosisRevision = new DiagnosisRevision();
		diagnosisRevision.setType(type);
		diagnosisRevision.setCreationDate(System.currentTimeMillis());
		diagnosisRevision.setName(
				TaskUtil.getDiagnosisRevisionName(parent.getDiagnosisRevisions(), diagnosisRevision, resourceBundle));

		addDiagnosisRevision(parent, diagnosisRevision);

		return diagnosisRevision;
	}

	public void addDiagnosisRevision(DiagnosisContainer parent, DiagnosisRevision diagnosisRevision) {

		diagnosisRevision.setParent(parent);
		parent.getDiagnosisRevisions().add(diagnosisRevision);
		diagnosisRevision.setSequenceNumber(parent.getDiagnosisRevisions().indexOf(diagnosisRevision));

		// saving to database
		genericDAO.savePatientData(diagnosisRevision, "log.patient.task.diagnosisContainer.diagnosisRevision.new",
				diagnosisRevision.toString());

		// creating a diagnosis for every sample
		for (Sample sample : parent.getParent().getSamples()) {
			createDiagnosis(diagnosisRevision, sample);
		}

	}

	/**
	 * Deleting a DiagnosisRevision and all included diagnoese
	 * 
	 * @param revision
	 * @return
	 */
	public DiagnosisRevision removeDiagnosisRevision(DiagnosisRevision revision) {
		logger.info("Removing diagnosisRevision " + revision.getName());

		revision.getParent().getDiagnosisRevisions().remove(revision);

		genericDAO.deletePatientData(revision, "log.patient.task.diagnosisContainer.diagnosisRevision.delete",
				revision.toString());

		return revision;
	}

	/**
	 * Updates a diagnosisRevision with a sample list. Used for adding and removing
	 * samples after initial revision creation.
	 * 
	 * @param diagnosisRevision
	 * @param samples
	 */
	public void updateDiagnosisRevisionToSampleCount(DiagnosisRevision diagnosisRevision, List<Sample> samples) {
		logger.info("Updating diagnosis list with new sample list");

		List<Diagnosis> diagnosesInRevision = diagnosisRevision.getDiagnoses();

		List<Sample> samplesToAddDiagnosis = new ArrayList<Sample>(samples);

		List<Diagnosis> toRemoveDiagnosis = new ArrayList<Diagnosis>();

		outerLoop: for (Diagnosis diagnosis : diagnosesInRevision) {
			// sample already in diagnosisList, removing from to add array
			for (Sample sample : samplesToAddDiagnosis) {
				if (sample.getId() == diagnosis.getSample().getId()) {
					samplesToAddDiagnosis.remove(sample);
					logger.trace("Sample found, Removing sample " + sample.getId() + " from list.");
					continue outerLoop;
				}
			}
			logger.trace("Diagnosis has no sample, removing diagnosis " + diagnosis.getId());
			// not found within samples, so sample was deleted, deleting
			// diagnosis as well.
			toRemoveDiagnosis.add(diagnosis);
		}

		// removing diagnose if necessary
		for (Diagnosis diagnosis : toRemoveDiagnosis) {
			removeDiagnosis(diagnosis);
		}

		// adding new diagnoses if there are new samples
		for (Sample sample : samplesToAddDiagnosis) {
			logger.trace("Adding new diagnosis for sample " + sample.getId());
			createDiagnosis(diagnosisRevision, sample);
		}

		genericDAO.savePatientData(diagnosisRevision, "log.patient.task.diagnosisContainer.diagnosisRevision.new",
				diagnosisRevision);

	}

	public void copyHistologicalRecord(Diagnosis tmpDiagnosis, boolean overwrite)
			throws CustomDatabaseInconsistentVersionException {
		logger.debug("Setting extended diagnosistext text");
		tmpDiagnosis.getParent()
				.setText(overwrite ? tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText()
						: tmpDiagnosis.getParent().getText() + "\r\n"
								+ tmpDiagnosis.getDiagnosisPrototype().getExtendedDiagnosisText());

		genericDAO.savePatientData(tmpDiagnosis.getParent(),
				"log.patient.task.diagnosisContainer.diagnosisRevision.update", tmpDiagnosis.getParent().toString());
	}

	/********************************************************
	 * Diagnosis Revision
	 ********************************************************/

	/********************************************************
	 * Sample Manipulation
	 ********************************************************/
	/**
	 * Creates a new sample and adds this sample to the given task. Creates a new
	 * diagnosis and a new block with slides as well.
	 * 
	 * @param task
	 */
	public void createNewSample(Task task, MaterialPreset material) {
		Sample sample = new Sample(task, material);

		genericDAO.savePatientData(sample, "log.patient.task.sample.new", sample);

		// creating needed blocks
		createNewBlock(sample, false);

		genericDAO.savePatientData(sample, "log.patient.task.sample.update", sample);

		// creating first default diagnosis
		updateDiagnosisContainerToSampleCount(task.getDiagnosisContainer(), task.getSamples());
	}

	/********************************************************
	 * Sample Manipulation
	 ********************************************************/

	/********************************************************
	 * Block Manipulation
	 ********************************************************/

	/**
	 * Creates a new block for the given sample. Adds all slides from the material
	 * preset to the block.
	 * 
	 * @param sample
	 * @param material
	 */
	public Block createNewBlock(Sample sample, boolean useAutoNomenclature) {
		Block block = new Block();
		block.setParent(sample);
		sample.getBlocks().add(block);

		genericDAO.savePatientData(block, "log.patient.task.sample.blok.new", block.getBlockID());

		logger.debug("Creating new block " + block.getBlockID());

		for (StainingPrototype proto : sample.getMaterilaPreset().getStainingPrototypes()) {
			createSlide(proto, block);
		}

		block.updateAllNames(useAutoNomenclature, false);

		genericDAO.savePatientData(block, "log.patient.task.sample.block.update", block.getBlockID());

		return block;
	}

	/********************************************************
	 * Block Manipulation
	 ********************************************************/

	/********************************************************
	 * Staining Manipulation
	 ********************************************************/
	/**
	 * Adds a new staining to a block. Needs the sample an the patient for logging.
	 * Commentary will be null.
	 * 
	 * @param prototype
	 * @param sample
	 * @param block
	 * @param patientOfSample
	 */
	public void createSlide(StainingPrototype prototype, Block block) {
		createSlide(prototype, block, null, false);
	}

	/**
	 * Adds a new staining to a block. Needs the sample an the patient for logging.
	 * Commentary the given string.
	 * 
	 * @param prototype
	 * @param sample
	 * @param block
	 * @param commentary
	 * @param patientOfSample
	 */
	public void createSlide(StainingPrototype prototype, Block block, String commentary, boolean reStaining) {
		logger.debug("Creating new slide " + prototype.getName());

		Slide slide = new Slide();

		slide.setCreationDate(System.currentTimeMillis());
		slide.setSlidePrototype(prototype);
		slide.setParent(block);

		// setting unique slide number
		slide.setUniqueIDinTask(block.getTask().getNextSlideNumber());

		block.getSlides().add(slide);

		slide.updateNameOfSlide(block.getTask().isUseAutoNomenclature(), false);

		if (commentary != null && !commentary.isEmpty())
			slide.setCommentary(commentary);

		slide.setReStaining(reStaining);

		genericDAO.savePatientData(slide, "log.patient.task.sample.block.slide.new", slide.toString());

	}

	/********************************************************
	 * Staining Manipulation
	 ********************************************************/
}
