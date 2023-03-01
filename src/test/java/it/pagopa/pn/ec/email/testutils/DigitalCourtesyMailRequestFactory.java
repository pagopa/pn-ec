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

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private static String getTimeStamp() {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String val = sdf.format(timestamp);
		return " [" + val + "]";
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
		digitalCourtesyMailRequestFactory.setSubjectText(SUBJECT + DigitalCourtesyMailRequestFactory.getTimeStamp());
		digitalCourtesyMailRequestFactory.setMessageContentType(MessageContentTypeEnum.HTML);
		digitalCourtesyMailRequestFactory.setMessageText(MESSAGE);

		List<String> attachList = new ArrayList<>();
		for (int idx = 1; idx <= attachNum; idx++) {
			attachList.add("C:\\Temp\\example.pdf");
		}
		digitalCourtesyMailRequestFactory.setAttachmentsUrls(attachList);

		return digitalCourtesyMailRequestFactory;
	}

}
