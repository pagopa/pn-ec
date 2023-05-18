package it.pagopa.pn.ec.email.testutils;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.ChannelEnum.EMAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum;

public class DigitalCourtesyMailRequestFactory {

	static final String ADDRESS_SEND = "filippo.forcina@dgsspa.com";
	static final String ADDRESS_RECIVE = "filippo.forcina@dgsspa.com";
	static final String SUBJECT = "Amazon SES Test";
	static final String MESSAGE = "This email was sent through Amazon SES by using the AWS SDK for Java.";
	static final String CONTENT = "<!DOCTYPE html>\n" //
			+ "<html lang=\"en\">\n" //
			+ "<head>\n" //
			+ "    <meta charset=\"utf-8\">\n"//
			+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" //
			+ "    <title>Example HTML Email with multiple attachents</title>\n" //
			+ "</head>\n"//
			+ "<body style=\"background: whitesmoke; padding: 30px; height: 100%\">\n" //
			+ "<h5 style=\"font-size: 18px; margin-bottom: 6px\">Dear example,</h5>\n" //
			+ "<p style=\"font-size: 16px; font-weight: 500\">Greetings from TutorialsBuddy</p>\n" //
			+ "<p>This is a simple html based email with multiple attachments.</p>\n" //
			+ "</body>\n" //
			+ "</html>";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private static String getTimeStamp() {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String val = sdf.format(timestamp);
		return "[" + val + "]";
	}

	public static DigitalCourtesyMailRequest createMailRequest(int attachNum) {
		String defaultStringInit = "string";

		var digitalCourtesyMailRequestFactory = new DigitalCourtesyMailRequest();
		digitalCourtesyMailRequestFactory.setRequestId(DEFAULT_REQUEST_IDX);
		digitalCourtesyMailRequestFactory.eventType(defaultStringInit);
		digitalCourtesyMailRequestFactory.setClientRequestTimeStamp(OffsetDateTime.now());
		digitalCourtesyMailRequestFactory.setQos(INTERACTIVE);
		digitalCourtesyMailRequestFactory.channel(EMAIL);
		digitalCourtesyMailRequestFactory.setSenderDigitalAddress(ADDRESS_SEND);
		digitalCourtesyMailRequestFactory.setReceiverDigitalAddress(ADDRESS_RECIVE);
		digitalCourtesyMailRequestFactory.setSubjectText(SUBJECT + " with #" + attachNum + " attach " + DigitalCourtesyMailRequestFactory.getTimeStamp());
		digitalCourtesyMailRequestFactory.setMessageText(CONTENT);
		digitalCourtesyMailRequestFactory.setMessageContentType(MessageContentTypeEnum.HTML);

		List<String> attachList = new ArrayList<>();
		for (int idx = 0; idx < attachNum; idx++) {
			attachList.add("safestorage://PN_EXTERNAL_LEGAL_FACTS-14d277f9beb4c8a9da322092c350d51");
		}
		digitalCourtesyMailRequestFactory.setAttachmentUrls(attachList);

		return digitalCourtesyMailRequestFactory;
	}

}
