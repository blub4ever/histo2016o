package org.histo.model.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.model.interfaces.HasID;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "settings_sequencegenerator", sequenceName = "settings_sequence")
@Getter
@Setter
public class HistoSettings implements HasID, Cloneable {

	@Id
	@GeneratedValue(generator = "settings_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * True if the printer should be autoselected
	 */
	@Column
	private boolean autoSelectedPreferedPrinter;

	/**
	 * Name of the preferred cups printer
	 */
	@Column(columnDefinition = "VARCHAR")
	private String preferedPrinter;

	/**
	 * True if the label printer should be autoselected
	 */
	@Column
	private boolean autoSelectedPreferedLabelPrinter;

	/**
	 * The uuid of the preferred labelprinter
	 */
	@Column(columnDefinition = "VARCHAR")
	private String preferedLabelPritner;

	/**
	 * Page which should be shown as default page
	 */
	@Enumerated(EnumType.ORDINAL)
	private View defaultView;

	/**
	 * Default worklist to load, staining, diagnosis, notification, none
	 */
	@Enumerated(EnumType.ORDINAL)
	private SimpleSearchOption worklistToLoad;

	/**
	 * Default sortorder of worklist
	 */
	@Enumerated(EnumType.ORDINAL)
	private WorklistSortOrder worklistSortOrder;

	/**
	 * True the sort order is ascending, false the sortorder is descending
	 */
	@Column
	private boolean worklistSortOrderAsc;

	/**
	 * If true none active tasks in worklist will be hidden per default
	 */
	@Column
	private boolean worklistHideNoneActiveTasks;

	/**
	 * True if autoupdate of the current worklist should be happening
	 */
	@Column
	private boolean worklistAutoUpdate;

	/**
	 * If true, a patient added viea quciksearch will be added to the worklist
	 * an the create task dialog will be opend.
	 */
	@Column
	private boolean alternatePatientAddMode;

	/**
	 * Background-Color of all inputfields
	 */
	@Column(columnDefinition = "VARCHAR", length = 7)
	private String inputFieldColor;

	/**
	 * Font-Color of the inputfields
	 */
	@Column(columnDefinition = "VARCHAR", length = 7)
	private String inputFieldFontColor;

	/**
	 * If true a pdf preview will be generated on diagnosis approval.
	 */
	@Column
	private boolean pdfPreviewOnDiagnosisApproval;
	
	/**
	 * List of available views
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Fetch(value = FetchMode.SUBSELECT)
	@Cascade(value = { org.hibernate.annotations.CascadeType.ALL })
	@OrderColumn(name = "position")
	private List<View> availableViews;

	@Transient
	public View[] getAvailableViewsAsArray() {
		return (View[]) getAvailableViews().toArray(new View[getAvailableViews().size()]);
	}

	public void setAvailableViewsAsArray(View[] views) {
		this.availableViews = Arrays.asList(views);
	}

	@Override
	protected HistoSettings clone() throws CloneNotSupportedException {
		return (HistoSettings) super.clone();
	}

	/**
	 * Only copies crucial settings
	 * 
	 * @param newSettings
	 */
	public void updateCrucialSettings(HistoSettings newSettings) {
		setDefaultView(newSettings.getDefaultView());
		setWorklistToLoad(newSettings.getWorklistToLoad());
		setWorklistSortOrder(newSettings.getWorklistSortOrder());
		setWorklistSortOrderAsc(newSettings.isWorklistSortOrderAsc());
		setWorklistHideNoneActiveTasks(newSettings.isWorklistHideNoneActiveTasks());
		setAlternatePatientAddMode(newSettings.isAlternatePatientAddMode());
		setAvailableViews(new ArrayList<View>(newSettings.getAvailableViews()));
	}

}
