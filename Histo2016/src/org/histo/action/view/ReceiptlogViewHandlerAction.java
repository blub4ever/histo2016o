package org.histo.action.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.settings.SettingsDialogHandler;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StainingListAction;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.ListItem;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateSlideLable;
import org.histo.ui.StainingTableChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
@Getter
@Setter
public class ReceiptlogViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	@Transient
	private ArrayList<StainingTableChooser<?>> stainingTableRows;

	/**
	 * This variable is used to save the selected action, which should be executed
	 * upon all selected slides
	 */
	private StainingListAction actionOnMany;

	/**
	 * Is used for selecting a chooser from the generated list (generated by task).
	 * It is used to edit the names of the entities by an overlaypannel
	 */
	private StainingTableChooser<?> selectedStainingTableChooser;

	public void prepareForTask(Task task) {
		logger.debug("Initilize ReceiptlogViewHandlerAction for task");
		// generating guilist for display
		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Updates the flat task/sample/block/slide list
	 * @param task
	 * @param showArchived
	 */
	public void updateSlideGuiList(Task task, boolean showArchived) {
		setStainingTableRows(StainingTableChooser.factory(task, showArchived));
	}

	/**
	 * Executes an action on all selected slides
	 * 
	 * @param task
	 */
	public void performActionOnManyTaskChildren(Task task) {
		performActionOnManyTaskChildren(task, getActionOnMany());
	}

	/**
	 * Executes an action on all selected slides
	 * 
	 * @param list
	 * @param action
	 */
	public void performActionOnManyTaskChildren(Task task, StainingListAction action) {
		try {
			List<StainingTableChooser<?>> list = getStainingTableRows();

			// at least one thing has to bee selected
			boolean atLeastOnechoosen = false;
			for (StainingTableChooser<?> stainingTableChooser : list) {
				if (stainingTableChooser.isChoosen()) {
					atLeastOnechoosen = true;
					break;
				}
			}

			if (!atLeastOnechoosen) {
				logger.debug("Nothing selected, do not performe any action");
				return;
			}

			switch (getActionOnMany()) {
			case PERFORMED:
				logger.debug("Setting staining status of selected slides to perforemd!");

				sampleService
						.setStainingCompletedForSlides(list.stream().filter(p -> p.isChoosen() && p.isStainingType())
								.map(p -> (Slide) p.getEntity()).collect(Collectors.toList()), true);

				// shows dialog for informing the user that all stainings are
				// performed
				if (sampleService.updateStaingPhase(task))
					dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(task);

				break;
			case NOT_PERFORMED:
				logger.debug("Setting staining status of selected slides to not perforemd!");

				sampleService
						.setStainingCompletedForSlides(list.stream().filter(p -> p.isChoosen() && p.isStainingType())
								.map(p -> (Slide) p.getEntity()).collect(Collectors.toList()), false);

				if (sampleService.updateStaingPhase(task))
					dialogHandlerAction.getStainingPhaseExitDialog().initAndPrepareBean(task);

				break;
			case ARCHIVE:
				// TODO implement
				System.out.println("To impliment");
				break;
			case PRINT:

				DocumentTemplate[] arr = DocumentTemplate.getTemplates(DocumentType.LABLE);

				if (arr.length == 0 || !(arr[0] instanceof TemplateSlideLable)) {
					logger.debug("No template found for printing, returning!");
					return;
				}

				TemplateSlideLable printTemplate = (TemplateSlideLable) arr[0];

				logger.debug("Printing labes for selected slides");

				List<TemplateSlideLable> toPrint = new ArrayList<TemplateSlideLable>();

				for (StainingTableChooser<?> stainingTableChooser : list) {
					if (stainingTableChooser.isChoosen() && stainingTableChooser.isStainingType()) {

						Slide slide = (Slide) stainingTableChooser.getEntity();

						TemplateSlideLable tmp = (TemplateSlideLable) printTemplate.clone();
						tmp.initData(task, slide, new Date(System.currentTimeMillis()));
						tmp.fillTemplate();
						toPrint.add(tmp);
					}
				}

				if (toPrint.size() != 0) {
					try {
						userHandlerAction.getSelectedLabelPrinter().print(toPrint);
					} catch (CustomUserNotificationExcepetion e) {
						// handling offline error
						mainHandlerAction.addQueueGrowlMessageAsResource(e);
					}
				}

				break;
			default:
				break;
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.onVersionConflictTask();
		}

		setActionOnMany(StainingListAction.NONE);

	}

	/**
	 * Toggles the status of a StainingTableChooser object and all chides.
	 * 
	 * @param chooser
	 */
	public void toggleChildrenChoosenFlag(StainingTableChooser<?> chooser) {
		setChildrenAsChoosen(chooser, !chooser.isChoosen());
	}

	/**
	 * Sets all children of a StainingTableChoosers to chosen/unchosen
	 * 
	 * @param chooser
	 * @param choosen
	 */
	public void setChildrenAsChoosen(StainingTableChooser<?> chooser, boolean chosen) {
		chooser.setChoosen(chosen);
		if (chooser.isSampleType() || chooser.isBlockType()) {
			for (StainingTableChooser<?> tmp : chooser.getChildren()) {
				setChildrenAsChoosen(tmp, chosen);
			}
		}

		setActionOnMany(StainingListAction.NONE);
	}

	/**
	 * Sets a lists of StainingTableChoosers to chosen/unchosen Setzt den Status
	 * einer Liste von StainingTableChoosers und ihrer Kinder
	 * 
	 * @param choosers
	 * @param choosen
	 */
	public void setListAsChoosen(List<StainingTableChooser<?>> choosers, boolean chosen) {
		for (StainingTableChooser<?> chooser : choosers) {
			if (chooser.isSampleType()) {
				setChildrenAsChoosen(chooser, chosen);
			}
		}
	}

	/**
	 * Prints a lable for the choosen slide.
	 * 
	 * @param slide
	 */
	public void printLableForSlide(Slide slide) {

		DocumentTemplate[] arr = DocumentTemplate.getTemplates(DocumentType.LABLE);

		if (arr.length == 0 || !(arr[0] instanceof TemplateSlideLable)) {
			logger.debug("No template found for lable printn!");
			return;
		}

		TemplateSlideLable printTemplate = (TemplateSlideLable) arr[0];
		printTemplate.prepareTemplate();
		printTemplate.initData(slide.getTask(), slide, new Date(System.currentTimeMillis()));
		printTemplate.fillTemplate();

		userHandlerAction.getSelectedLabelPrinter().print(printTemplate);

	}

	/**
	 * Saves the manually altered flag, if the sample/block/ or slide id was
	 * manually altered.
	 * 
	 * @param idManuallyAltered
	 * @param altered
	 */
	public void onEntityIDAlteredOverlayClose(StainingTableChooser<?> chooser) {

		// checking if something was altered, if not do nothing
		if (chooser.isIdChanged()) {
			try {

				chooser.getEntity().setIdManuallyAltered(true);

				chooser.getEntity().updateAllNames(chooser.getEntity().getTask().isUseAutoNomenclature(), false);

				// TODO update childrens names
				genericDAO.savePatientData(chooser.getEntity(), "log.patient.task.idManuallyAltered",
						chooser.getEntity().toString());

			} catch (CustomDatabaseInconsistentVersionException e) {
				// catching database version inconsistencies
				worklistViewHandlerAction.onVersionConflictTask();
			}
			chooser.setIdChanged(false);
		}
	}
}
