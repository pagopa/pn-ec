package it.pagopa.pn.ec.sms.testutils;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;

import java.time.OffsetDateTime;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;

public class DigitalCourtesySmsRequestFactory {

    public static DigitalCourtesySmsRequest createSmsRequest(){
        String defaultStringInit = "string";

        var digitalCourtesySmsRequestFactory= new DigitalCourtesySmsRequest();
        digitalCourtesySmsRequestFactory.setRequestId(DEFAULT_REQUEST_IDX);
        digitalCourtesySmsRequestFactory.eventType(defaultStringInit);
        digitalCourtesySmsRequestFactory.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalCourtesySmsRequestFactory.setQos(INTERACTIVE);
        digitalCourtesySmsRequestFactory.setReceiverDigitalAddress("+393890091180");
        digitalCourtesySmsRequestFactory.setMessageText(defaultStringInit);
        digitalCourtesySmsRequestFactory.channel(SMS);
        return digitalCourtesySmsRequestFactory;
    }
}
