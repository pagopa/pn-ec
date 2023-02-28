package it.pagopa.pn.ec.email.testutils;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.ChannelEnum.EMAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;

import java.time.OffsetDateTime;
import java.util.Arrays;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.MessageContentTypeEnum;

public class DigitalCourtesyMailRequestFactory {

	public static DigitalCourtesyMailRequest createMailRequest() {
		String defaultStringInit = "string";

		var digitalCourtesyMailRequestFactory = new DigitalCourtesyMailRequest();
		digitalCourtesyMailRequestFactory.setRequestId(DEFAULT_REQUEST_IDX);
		digitalCourtesyMailRequestFactory.eventType(defaultStringInit);
		digitalCourtesyMailRequestFactory.setClientRequestTimeStamp(OffsetDateTime.now());
		digitalCourtesyMailRequestFactory.setQos(INTERACTIVE);
		digitalCourtesyMailRequestFactory.setReceiverDigitalAddress("+393890091180");
		digitalCourtesyMailRequestFactory.setMessageText(defaultStringInit);
		digitalCourtesyMailRequestFactory.channel(EMAIL);
		digitalCourtesyMailRequestFactory.setSubjectText(defaultStringInit);
		digitalCourtesyMailRequestFactory.setMessageContentType(MessageContentTypeEnum.PLAIN);
		digitalCourtesyMailRequestFactory.setAttachmentsUrls(Arrays.asList(defaultStringInit));

		return digitalCourtesyMailRequestFactory;
	}

}
