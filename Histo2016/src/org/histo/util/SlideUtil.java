package org.histo.util;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.Block;
import org.histo.model.Sample;
import org.histo.model.Staining;
import org.histo.model.StainingPrototype;
import org.histo.model.Task;
import org.histo.ui.StainingListChooser;

public class SlideUtil {

    /**
     * Erstellt einen Liste mit F�rbungen, die ausgew�hlt werden k�nnen um sie einem Block hinzuzuf�gen
     * 
     * @param stainingPrototypes
     * @return
     */
    public final static ArrayList<StainingListChooser> getStainingListChooser(List<StainingPrototype> stainingPrototypes) {
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

	    lone: for (Staining slide : block.getStainings()) {
		
		// weiter, wenn slide archiviert wurde
		if (slide.isArchived())
		    continue;

		atLeastOneSlide = true;
		break lone;
	    }
	}
	
	return atLeastOneSlide;
    }
}