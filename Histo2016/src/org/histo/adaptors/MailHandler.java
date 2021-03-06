package org.histo.adaptors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.model.interfaces.GsonAble;
import org.histo.template.MailTemplate;
import org.histo.template.TemplateDeserializer;
import org.histo.util.FileUtil;
import org.histo.util.StreamUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailHandler implements GsonAble {

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private static Logger logger = Logger.getLogger("org.histo");

	private MailSettings settings;

	private String[] adminRecipients;

	private String[] errorRecipients;
	
	public static <T extends MailTemplate> T getDefaultTemplate(Class<T> mailType) {
		return getTemplates(mailType).stream().filter(p -> p.isDefaultOfType()).collect(StreamUtils.singletonCollector());
	}

	public static <T extends MailTemplate> List<T> getTemplates(Class<T> mailType) {

		// TODO move to Database
		Type type = new TypeToken<List<MailTemplate>>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer<MailTemplate>());

		Gson gson = gb.create();

		ArrayList<MailTemplate> jsonArray = gson
				.fromJson(FileUtil.getContentOfFile(GlobalSettings.MAIL_TEMPLATES), type);

		List<T> result = new ArrayList<T>();

		for (MailTemplate mailTemplate : jsonArray) {
			if (mailTemplate.getTemplateName().equals(mailType.getName())) {
				result.add((T)mailTemplate);
			}
		}

		return result;
	}

	public boolean sendErrorMail(MailTemplate mail) {
		return sendMail(getErrorRecipients(), settings.getSystemMail(), settings.getSystemName(), mail);
	}

	public boolean sendAdminMail(MailTemplate mail) {
		return sendMail(getAdminRecipients(), settings.getSystemMail(), settings.getSystemName(), mail);
	}
	
	public boolean sendMail(String mailTo, MailTemplate template) {
		return sendMail(new String[] {mailTo} , settings.getSystemMail(), settings.getSystemName(), template);
	}

	public boolean sendMail(List<String> mailTo, MailTemplate template) {
		return sendMail(mailTo, settings.getSystemMail(), settings.getSystemName(), template);
	}

	public boolean sendMail(String[] mailTo, String mailFrom, String nameFrom, MailTemplate mail) {
		return sendMail(Arrays.asList(mailTo), mailFrom, nameFrom, mail);
	}

	public boolean sendMail(List<String> mailTo, String mailFrom, String nameFrom, MailTemplate mail) {

		if (mail.getAttachment() == null) {
			SimpleEmail email = new SimpleEmail();

			email.setHostName(settings.getServer());
			email.setDebug(settings.isDebug());
			email.setSmtpPort(settings.getPort());
			email.setSSLOnConnect(settings.isSsl());

			try {
				for (String to : mailTo) {
					email.addTo(to);
				}

				email.setFrom(mailFrom, nameFrom);

				email.setSubject(mail.getSubject());
				email.setMsg(mail.getBody());
				email.send();
			} catch (EmailException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			// Create the email message
			MultiPartEmail email = new MultiPartEmail();

			email.setHostName(settings.getServer());
			email.setDebug(settings.isDebug());
			email.setSmtpPort(settings.getPort());
			email.setSSLOnConnect(settings.isSsl());

			try {
				for (String to : mailTo) {
					email.addTo(to);
				}
				email.setFrom(mailFrom, nameFrom);

				email.setSubject(mail.getSubject());
				email.setMsg(mail.getBody());

				InputStream is = new ByteArrayInputStream(mail.getAttachment().getData());

				DataSource source = new ByteArrayDataSource(is, "application/pdf");

				// add the attachment
				email.attach(source, mail.getAttachment().getName(), "");

				// send the email
				email.send();
			} catch (EmailException | IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	@Getter
	@Setter
	public class MailSettings {

		private String server;

		private int port;

		private boolean ssl;

		private boolean debug;

		private String systemMail;

		private String systemName;
	}
}
