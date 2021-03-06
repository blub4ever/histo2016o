package org.histo.adaptors.printer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.transitory.settings.DefaultDocuments;
import org.histo.model.transitory.settings.PrinterSettings;
import org.histo.template.DocumentTemplate;
import org.histo.util.FileUtil;
import org.histo.util.HistoUtil;
import org.histo.util.pdf.PDFGenerator;
import org.histo.util.pdf.PDFUtil;
import org.histo.util.pdf.PrintOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lowagie.text.pdf.PdfWriter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Configurable
public class ClinicPrinter extends AbstractPrinter {

	private static String IPADDRESS_PATTERN = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	protected PrinterSettings settings;

	protected DefaultDocuments defaultDocuments;

	public ClinicPrinter() {
	}

	public ClinicPrinter(CupsPrinter cupsPrinter, PrinterSettings settings, DefaultDocuments defaultDocuments) {
		this.id = cupsPrinter.getName().hashCode();
		this.address = cupsPrinter.getPrinterURL().toString();
		this.name = cupsPrinter.getName();
		this.description = cupsPrinter.getDescription();
		this.location = cupsPrinter.getLocation();
		this.settings = settings;
		this.defaultDocuments = defaultDocuments;

		// getting ip
		Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
		Matcher matcher = pattern.matcher(cupsPrinter.getDeviceURI());

		if (matcher.find()) {
			this.deviceUri = matcher.group();
		} else {
			this.deviceUri = "0.0.0.0";
		}
	}

	/**
	 * Prints a file
	 * 
	 * @param cPrinter
	 * @param file
	 */
	public boolean print(File file) {
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
			CupsPrinter printer = cupsClient.getPrinter(new URL(address));
			PrintJob printJob = new PrintJob.Builder(FileUtils.readFileToByteArray(file)).build();
			printer.print(printJob);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean print(PDFContainer container) {
		return print(container, "");
	}

	public boolean print(PDFContainer container, int count) {
		return print(container, count, null);
	}

	public boolean print(PDFContainer container, String args) {
		return print(container, 1, args);
	}

	public boolean print(PDFContainer container, DocumentTemplate template) {
		return print(new PrintOrder(container, template));
	}

	public boolean print(PDFContainer container, int copies, String args) {
		return print(new PrintOrder(container, copies, false, args));
	}

	public boolean print(PrintOrder printOrder) {
		logger.debug("Printing xtimes: " + printOrder.getCopies());
		int i = 0;
		boolean result = true;
		logger.debug("Printing " + i);
		logger.debug("Printing to printer " + settings.getCupsHost());
		
		CupsClient cupsClient;
		try {
			cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
			CupsPrinter printer = cupsClient.getPrinter(new URL(address));

			// adding additional page if duplex print an odd pages are provided
//			if (PDFGenerator.countPDFPages(printOrder.getPdfContainer()) % 2 != 0 && printOrder.isDuplex()) {
//				byte[] arr = FileUtil.getFileAsBinary(defaultDocuments.getEmptyPage());
//
//				PDFContainer cont = PDFGenerator.mergePdfs(
//						Arrays.asList(printOrder.getPdfContainer(), new PDFContainer(DocumentType.EMPTY, arr)), "",
//						DocumentType.PRINT_DOCUMENT);
//
//				
//				printOrder.setPdfContainer(cont);
//				
//				logger.debug("Printing in duplex mode, adding empty page at the end");
//			}

			System.out.println(printOrder.getPdfContainer());
			System.out.println(printOrder.getPdfContainer().getData().length);
			
			PrintJob printJob = new PrintJob.Builder(new ByteArrayInputStream(printOrder.getPdfContainer().getData()))
					.duplex(printOrder.isDuplex()).copies(printOrder.getCopies()).build();

			if (HistoUtil.isNotNullOrEmpty(printOrder.getArgs())) {

				Map<String, String> attribute = new HashMap<String, String>();
				attribute.put("job-attributes", printOrder.getArgs());

				printJob.setAttributes(attribute);
				logger.debug("Printig with args: " + printOrder.getArgs());
			}

			printer.print(printJob);

		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		}
		i++;

		return result;
	}

	public boolean printTestPage() {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(settings.getTestPage());
		try {
			print(new File(resource.getURI()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			appContext.close();
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClinicPrinter && ((ClinicPrinter) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	public static String printerToJson(ClinicPrinter clinicPrinter) {
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return gson.toJson(clinicPrinter);

	}

	public static ClinicPrinter getPrinterFromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, ClinicPrinter.class);
	}
}
