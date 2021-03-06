package org.histo.adaptors.clinic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.histo.adaptors.AbstractJsonHandler;
import org.histo.config.exception.CustomExceptionToManyEntries;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.AssociatedContact;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Patient;
import org.histo.util.TimeUtil;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

//{
// "vorname":"Andreas",
// "mode":null,
// "status":null,
// "piz":"20366346",
// "sonderinfo":null,
// "iknr":null,
// "kvnr":null,
// "titel":null,
// "versichertenstatus":" ",
// "tel":null,
// "anschrift":"Tannenweg 34",
// "wop":null,
// "plz":"79183",
// "name":"Glatz",
// "geburtsdatum":"1988-10-04",
// "gueltig_bis":null,
// "krankenkasse":null,
// "versnr":null,
// "land":"D",
// "weiblich":"",
// "ort":"Waldkirch",
// "status2":null
//}

//{
//
//+      "vorname":"Ioana Maria",
//->       "mode":"K",
//->       "status":"1000",
//ok      "piz":"29017379",
//?      "sonderinfo":"",
//->      "iknr":"61125",
//->      "kvnr":"108018121",
//ok      "titel":null,
//->      "versichertenstatus":"1 ",
//ok     "tel":"0176 62346167",
//ok      "anschrift":"Habsburgerstr. 25",
//?      "wop":"",
//ok      "plz":"79104",
//ok      "name":"Cazana",
//ok      "geburtsdatum":"1989-10-09",
//?     "gueltig_bis":null,
//ok      "krankenkasse":"AOK Baden-W�rttemberg",
//?     "versnr":"U367703198",
//ok      "land":"D",
//ok      "weiblich":"1",
//ok      "ort":"Freiburg",
//"status2":"1"
//
//}

@Getter
@Setter
public class ClinicJsonHandler extends AbstractJsonHandler {

	/**
	 * Loaded from json (/$piz) Replace $piz
	 */
	private String patientByPiz_URI;

	/**
	 * List of all hospitalized cases Replace $piz
	 */
	private String patientHistoryHospitalized_URI;

	/**
	 * List of all ambulant cases Replace $piz
	 */
	private String patientHistoryAmbulant_URI;

	/**
	 * Data of the current case / associated physicians Replace $piz $case
	 */
	private String patientAssocitedPersons_URI;

	/**
	 * Patient by name birthday or surname Replace $name $surname $birthday
	 */
	private String patientByNameSurnameBirthday_URI;

	/**
	 * Creates a list of patients fetched from the clinic backend. If there is no
	 * data returned due to to many results an error will be thrown.
	 * 
	 * @param url
	 * @return
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 * @throws JSONException
	 */
	public List<Patient> getPatientsFromClinicJson(String url)
			throws CustomExceptionToManyEntries, JSONException, CustomNullPatientExcepetion {
		String result = requestJsonData(url);
		return createPatientsFromClinicJson(result);
	}

	/**
	 * Creates a list of patients by data of the clinic backend.
	 * 
	 * @param name
	 * @param surname
	 * @param birthday
	 * @return
	 * @throws JSONException
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 */
	public List<Patient> getPatientsFromClinicJson(String name, String surname, Date birthday)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		String result = requestJsonData(patientByNameSurnameBirthday_URI.replace("$name", name == null ? "" : name)
				.replace("$surname", surname == null ? "" : surname)
				.replace("$birthday", birthday != null ? TimeUtil.formatDate(birthday, "yyyy-MM-dd") : ""));
		return createPatientsFromClinicJson(result);
	}

	/**
	 * Creates a patient object from data fechted from the clinic backend.
	 * 
	 * @param url
	 * @return
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	public Patient getPatientFromClinicJson(String piz)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		String result = requestJsonData(patientByPiz_URI.replace("$piz", piz));
		return createPatientFromClinicJson(result);
	}

	public List<ClinicCase> getClinicCasesOfPatient(String piz) {
		String result = requestJsonData(patientHistoryAmbulant_URI.replace("$piz", piz));
		return ClinicCase.factory(result);
	}

	public List<AssociatedContact> getContactsForCase(String piz, String caseNumber) {
		String result = requestJsonData(patientAssocitedPersons_URI.replace("$piz", piz).replace("$case", caseNumber));
		return null;
	}

	/**
	 * Updates a given patient with data from the backend
	 * 
	 * @param patient
	 * @return
	 * @throws JSONException
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 */
	public boolean updatePatientFromClinicJson(Patient patient)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		Patient update = getPatientFromClinicJson(patient.getPiz());
		if (update != null)
			return patient.copyIntoObject(update);
		return false;
	}

	/**
	 * Phrases a json string an returns a patient list, throws error if due to to
	 * many results no data was returned.
	 * 
	 * @param json
	 *            [{},{}]
	 * @return
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 * @throws JSONException
	 */
	private List<Patient> createPatientsFromClinicJson(String json)
			throws CustomExceptionToManyEntries, JSONException, CustomNullPatientExcepetion {
		JSONArray arr = new JSONArray(json);
		ArrayList<Patient> patients = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			patients.add(createPatientFromClinicJson(arr.getJSONObject(i)));
		}
		return patients;
	}

	/**
	 * Creates a new patient using data from the clinic backend. Throws an error if
	 * no data are given.
	 * 
	 * @param json
	 * @return
	 * @throws CustomNullPatientExcepetion
	 * @throws CustomExceptionToManyEntries
	 * @throws JSONException
	 */
	private Patient createPatientFromClinicJson(String json)
			throws JSONException, CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		return createPatientFromClinicJson(new JSONObject(json));
	}

	/**
	 * Creates a new Patient using a json object contain data from the clinic
	 * backend. If the json string contains the error field an error will be thrown.
	 * 
	 * @param json
	 * @return
	 * @throws CustomExceptionToManyEntries
	 * @throws CustomNullPatientExcepetion
	 */
	private Patient createPatientFromClinicJson(JSONObject json)
			throws CustomExceptionToManyEntries, CustomNullPatientExcepetion {
		Patient patient = new Patient();
		patient.setPerson(new Person());
		patient.getPerson().setContact(new Contact());

		if (json.has("error"))
			throw new CustomExceptionToManyEntries();

		patient.copyIntoObject(json);

		return patient;
	}

}
