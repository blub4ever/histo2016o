package org.histo.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.histo.action.MainHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisStatus;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PdfTemplate;
import org.histo.model.transitory.PdfTemplate.CodeRectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSmartCopy;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class PdfGenerator {

	private ResourceBundle resourceBundle;

	private MainHandlerAction mainHandlerAction;

	public PdfGenerator(MainHandlerAction mainHandlerAction, ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.mainHandlerAction = mainHandlerAction;
	}

	public PDFContainer generatePdfForTemplate(Task task, PdfTemplate template, long dateOfReport,
			ContactRole addressPhysicianRole, Physician externalPhysician, Physician signingPhysician) {
		PDFContainer result = null;

		switch (template.getType()) {
		case "COUNCIL":
			if (task.getCouncil() != null) {
				result = generatePdf(task, template, task.getCouncil().getDateOfRequest(),
						task.getCouncil().getCouncilPhysician(), task.getCouncil().getPhysicianRequestingCouncil());
			}
			break;
		case "INTERNAL":
			result = generatePdf(task, template, task.getReport().getSignatureDate(), null, null);
			break;
		default:
			Physician addressPhysician = null;
			if (addressPhysicianRole == ContactRole.FAMILY_PHYSICIAN
					|| addressPhysicianRole == ContactRole.PRIVATE_PHYSICIAN) {
				Contact tmp = task.getPrimaryContact(addressPhysicianRole);
				addressPhysician = (tmp == null ? null : tmp.getPhysician());
			} else {
				addressPhysician = externalPhysician;
			}

			result = generatePdf(task, template, dateOfReport, addressPhysician, signingPhysician);
			break;
		}

		return result;
	}

	public PDFContainer generatePdf(Task task, PdfTemplate template, long dateOfReport, Physician addressPhysician,
			Physician signingPhysician) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfReader pdfReader = getPdfFile(template.getFileWithLogo());
		PdfStamper pdf = getPdfStamper(pdfReader, out);

		// header
		populateReportHead(pdf, task, dateOfReport);

		// address
		populateReportAddress(pdf, addressPhysician);

		// body
		populateBody(pdf, task, dateOfReport);

		// signature
		if (signingPhysician != null)
			populateSingleSignature(pdf, signingPhysician);

		// barcodes
		drawBarCodes(task, template, pdfReader, pdf);

		pdf.setFormFlattening(true);

		closePdf(pdfReader, pdf);

		String pdfName = (template.isNameAsResources() ? resourceBundle.get(template.getName()) : template.getName())
				+ "_" + task.getPatient().getPiz();

		return new PDFContainer(template.getType(),
				pdfName + "_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf",
				out.toByteArray());
	}

	public final void drawBarCodes(Task task, PdfTemplate template, PdfReader reader, PdfStamper stamper) {
		if (template.getPizCode() != null && task.getParent().getPiz() != null
				&& !task.getParent().getPiz().isEmpty()) {
			drawBarCode(reader, stamper, task.getParent().getPiz(), template.getPizCode());
		}

		if (template.getTaskCode() != null) {
			drawBarCode(reader, stamper, task.getTaskID(), template.getTaskCode());
		}
	}

	public final void populateSingleSignature(PdfStamper stamper, Physician physician) {
		setStamperField(stamper, "B_SIGANTURE", physician.getFullName());
	}

	public final void populateReportHead(PdfStamper stamper, Task task, long dateOfReport) {
		setStamperField(stamper, "H_Name",
				task.getParent().getPerson().getName() + ", " + task.getParent().getPerson().getSurname());
		setStamperField(stamper, "H_Birthday", resourceBundle.get("pdf.birthday") + " "
				+ mainHandlerAction.date(task.getParent().getPerson().getBirthday()));
		setStamperField(stamper, "H_Insurance", task.getParent().getInsurance());
		setStamperField(stamper, "H_PIZ", task.getParent().getPiz().isEmpty()
				? resourceBundle.get("pdf.address.piz.none") : task.getParent().getPiz());
		setStamperField(stamper, "H_Date", mainHandlerAction.date(dateOfReport));
	}

	public final void populateReportAddress(PdfStamper stamper, Physician physician) {
		StringBuffer contAdr = new StringBuffer();

		if (physician != null) {
			contAdr.append(physician.getGender() == 'w' ? resourceBundle.get("pdf.address.female")
					: resourceBundle.get("pdf.address.male") + "\r\n");
			contAdr.append(physician.getFullName() + "\r\n");
			contAdr.append(physician.getStreet() + " " + physician.getHouseNumber() + "\r\n");
			contAdr.append(physician.getPostcode() + " " + physician.getTown() + "\r\n");
		} else
			contAdr.append(resourceBundle.get("pdf.address.none"));

		setStamperField(stamper, "H_ADDRESS", contAdr.toString());
	}

	public final void populateBody(PdfStamper stamper, Task task, long dateOfReport) {

		List<Sample> samples = task.getSamples();

		StringBuffer material = new StringBuffer();
		StringBuffer diagonsisList = new StringBuffer();
		StringBuffer reDiagonsisList = new StringBuffer();

		for (Sample sample : samples) {
			material.append(sample.getSampleID() + " " + sample.getMaterial() + "\r\n");
			diagonsisList
					.append(sample.getSampleID() + " " + sample.getLastRelevantDiagnosis().getDiagnosis() + "\r\n");

			if (sample.getDiagnosisStatus() == DiagnosisStatus.RE_DIAGNOSIS_NEEDED)
				reDiagonsisList.append(sample.getSampleID() + " "
						+ sample.getLastRelevantDiagnosis().getDiagnosisRevisionText() + "\r\n");
		}

		setStamperField(stamper, "B_DATE", mainHandlerAction.date(dateOfReport));
		setStamperField(stamper, "B_Name",
				task.getParent().getPerson().getName() + ", " + task.getParent().getPerson().getSurname());
		setStamperField(stamper, "B_Birthday", resourceBundle.get("pdf.birthday") + " "
				+ mainHandlerAction.date(task.getParent().getPerson().getBirthday()));

		setStamperField(stamper, "B_SAMPLES", material.toString());
		setStamperField(stamper, "B_EDATE", mainHandlerAction.date(task.getDateOfSugeryAsDate()));
		setStamperField(stamper, "B_TASK_NUMBER", task.getTaskID());
		setStamperField(stamper, "B_PIZ", task.getParent().getPiz().isEmpty() ? "" : task.getParent().getPiz());

		setStamperField(stamper, "B_EYE", resourceBundle.get("enum.eye." + task.getEye().toString()));
		setStamperField(stamper, "B_HISTORY", task.getCaseHistory());
		setStamperField(stamper, "B_INSURANCE_NORMAL", task.getPatient().isPrivateInsurance() ? "0" : "1");
		setStamperField(stamper, "B_INSURANCE_PRIVATE", task.getPatient().isPrivateInsurance() ? "1" : "0");
		setStamperField(stamper, "B_WARD", task.getWard());
		setStamperField(stamper, "B_MALIGN", task.isMalign() ? "1" : "0");

		Contact privatePhysician = task.getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
		Contact surgeon = task.getPrimaryContact(ContactRole.SURGEON);

		setStamperField(stamper, "B_PRIVATE_PHYSICIAN",
				privatePhysician == null ? "" : privatePhysician.getPhysician().getFullName());
		setStamperField(stamper, "B_SURGEON", surgeon == null ? "" : surgeon.getPhysician().getFullName());
		setStamperField(stamper, "B_DIAGNOSIS", diagonsisList.toString());
		setStamperField(stamper, "B_DATE", mainHandlerAction.date(dateOfReport));

		if (task.getCouncil() != null && task.getCouncil().getCouncilPhysician() != null) {
			setStamperField(stamper, "B_COUNCIL", task.getCouncil().getCouncilPhysician().getFullName());
			setStamperField(stamper, "B_TEXT", task.getCouncil().getCouncilText());
			setStamperField(stamper, "B_APPENDIX", task.getCouncil().getAttachment());
		}

		if (task.getDiagnosisStatus() == DiagnosisStatus.RE_DIAGNOSIS_NEEDED) {
			setStamperField(stamper, "B_RE_DIAGNOSIS", "1");
			setStamperField(stamper, "B_RE_DIAGNOSIS_TEXT", reDiagonsisList.toString());
		}

		if (task.getReport() != null) {

			setStamperField(stamper, "B_HISTOLOGICAL_RECORD", task.getReport().getHistologicalRecord());

			if (task.getReport().getPhysicianToSign().getPhysician() != null) {
				setStamperField(stamper, "S_PHYSICIAN",
						task.getReport().getPhysicianToSign().getPhysician().getFullName());
				setStamperField(stamper, "S_PHYSICIAN_ROLE", task.getReport().getPhysicianToSign().getRole());
			}

			if (task.getReport().getConsultantToSign().getPhysician() != null) {
				setStamperField(stamper, "S_CONSULTANT",
						task.getReport().getConsultantToSign().getPhysician().getFullName());
				setStamperField(stamper, "S_CONSULTANT_ROLE", task.getReport().getConsultantToSign().getRole());
			}
		}
	}

	/**
	 * Loads a PdfReader from a file
	 * 
	 * @param path
	 * @return
	 */
	public static final PdfReader getPdfFile(String path) {
		PdfReader pdfTemplate = null;
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();

		Resource resource = appContext.getResource(path);

		System.out.println(path);
		try {
			pdfTemplate = new PdfReader(resource.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return pdfTemplate;
	}

	/**
	 * Needs an output stream and a pdfReader and returens a new pdf stamper.
	 * 
	 * @param reader
	 * @param out
	 * @return
	 */
	public static final PdfStamper getPdfStamper(PdfReader reader, ByteArrayOutputStream out) {
		PdfStamper stamper = null;
		try {
			stamper = new PdfStamper(reader, out);
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stamper;
	}

	/**
	 * Closed the pdfreader and the pdfstamper.
	 * 
	 * @param reader
	 * @param stamper
	 */
	public static final void closePdf(PdfReader reader, PdfStamper stamper) {
		try {
			stamper.close();
			reader.close();
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a code128 field with the passed content, adds it at the desired
	 * location in the pdf
	 * 
	 * @param reader
	 * @param stamper
	 * @param code128Str
	 * @param hight
	 * @param lenth
	 * @param x
	 * @param y
	 */
	public static final void generateCode128Field(PdfReader reader, PdfStamper stamper, String code128Str, int x, int y,
			float lenth, float hight) {
		PdfContentByte over = stamper.getOverContent(1);
		Rectangle pagesize = reader.getPageSize(1);

		Barcode128 code128 = new Barcode128();
		code128.setBarHeight(hight);
		code128.setFont(null);
		code128.setX(lenth);
		code128.setCode(code128Str);

		com.lowagie.text.pdf.PdfTemplate template = code128.createTemplateWithBarcode(over, Color.BLACK, Color.BLACK);

		over.addTemplate(template, pagesize.getLeft() + x, pagesize.getTop() - y);
		System.out.println("printred");
	}

	/**
	 * Sets a field in the pdf stamper.
	 * 
	 * @param stamper
	 * @param field
	 * @param content
	 */
	public static final void setStamperField(PdfStamper stamper, String field, String content) {
		try {
			stamper.getAcroFields().setField(field, content);
		} catch (IOException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints all stamper fields within the pdf File
	 * 
	 * @param stamper
	 */
	public static final void printAllStamperFields(PdfStamper stamper) {
		AcroFields fields = stamper.getAcroFields();

		Set<String> fldNames = fields.getFields().keySet();

		for (String fldName : fldNames) {
			System.out.println(fldName + ": " + fields.getField(fldName));
		}
	}

	/**
	 * Draws a given barcode into the pdf file
	 * 
	 * @param reader
	 * @param stamper
	 * @param piz
	 * @param codes
	 */
	public static final void drawBarCode(PdfReader reader, PdfStamper stamper, String codeStr, CodeRectangle[] codes) {
		for (CodeRectangle code : codes) {
			generateCode128Field(reader, stamper, codeStr, code.getX(), code.getY(), code.getWidth(), code.getHeight());
		}
	}

	public static final PDFContainer mergePdfs(List<PDFContainer> containers, String name, String type) {
		Document document = new Document();
		ByteOutputStream out = new ByteOutputStream();

		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(document, out);
			document.open();
			PdfContentByte cb = writer.getDirectContent();

			for (PDFContainer pdfContainer : containers) {
				PdfReader pdfReader = new PdfReader(pdfContainer.getData());
				for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
					document.newPage();
					// import the page from source pdf
					PdfImportedPage page = writer.getImportedPage(pdfReader, i);
					// add the page to the destination pdf
					cb.addTemplate(page, 0, 0);
				}
			}

			document.close();
		} catch (DocumentException | IOException e) {
			e.printStackTrace();
			return null;
		}

		return new PDFContainer(type, name, out.getBytes());
	}
}
