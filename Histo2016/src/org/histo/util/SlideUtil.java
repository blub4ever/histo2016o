package org.histo.util;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingListChooser;

public class SlideUtil {

	/**
	 * Erstellt einen Liste mit F�rbungen, die ausgew�hlt werden k�nnen um sie
	 * einem Block hinzuzuf�gen
	 * 
	 * @param stainingPrototypes
	 * @return
	 */
	public final static ArrayList<StainingListChooser> getStainingListChooser(
			List<StainingPrototype> stainingPrototypes) {
		ArrayList<StainingListChooser> res = new ArrayList<StainingListChooser>();
		for (StainingPrototype staining : stainingPrototypes) {
			res.add(new StainingListChooser(staining));
		}
		return res;
	}

	/**
	 * �berpr�ft einen Task ob alle Objekttr�ger gef�rbt wurden
	 * 
	 * @param task
	 * @return
	 */
	public final static boolean checkIfAtLeastOnSlide(Task task) {
		for (Sample sample : task.getSamples()) {
			if (!checkIfAtLeastOnSlide(sample))
				return false;
		}
		return true;
	}

	/**
	 * �berpr�ft eine Probe ob alle Objekttr�ger gef�rbt wurden
	 * 
	 * @param sample
	 * @return
	 */
	public final static boolean checkIfAtLeastOnSlide(Sample sample) {
		boolean atLeastOneSlide = false;

		for (Block block : sample.getBlocks()) {
			// weiter, wenn block archiviert wurde
			if (block.isArchived())
				continue;

			lone: for (Slide slide : block.getSlides()) {

				// weiter, wenn slide archiviert wurde
				if (slide.isArchived())
					continue;

				atLeastOneSlide = true;
				break lone;
			}
		}

		return atLeastOneSlide;
	}
	
	/**
	 * Checks if all slides are staind and stets the allStainingsPerformed flag
	 * in the task object to true.
	 * 
	 * @param sample
	 */
	public static final boolean checkIfAllSlidesAreStained(Task task) {
		boolean allPerformed = true;
		for (Sample sample : task.getSamples()) {
			// �bersprinen, wenn sample archiviert wurde
			if (sample.isArchived())
				continue;

			// nur als completed markieren wenn mindestens eine F�rbung
			// vorhanden ist
			if (sample.isStainingPerformed() && SlideUtil.checkIfAtLeastOnSlide(sample))
				sample.setReStainingPhase(true);
			else {
				allPerformed = false;
			}

		}

		if (allPerformed) {
			task.setStainingCompleted(true);
			task.setStainingCompletionDate(System.currentTimeMillis());
		} else
			task.setStainingCompleted(false);

		return allPerformed;
	}

}
