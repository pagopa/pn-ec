package it.pagopa.pn.ec.sms.testutils.factory;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;

import java.util.Date;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;

public class SmsRequestObjectFactory {

    private static final String DEFAULT_STRING_INIT = "string";
    private static final Date DEFAULT_DATE_INIT = new Date();

    /**
     * Default body per non far andare in 404 l'invio di un SMS
     *
     * @return Body con i campi valorizzati
     */
    public static DigitalCourtesySmsRequest getDigitalCourtesySmsRequest() {
        var request = new DigitalCourtesySmsRequest();
        request.setRequestId(DEFAULT_STRING_INIT);
        request.eventType(DEFAULT_STRING_INIT);
        request.setQos(INTERACTIVE);
        request.setClientRequestTimeStamp(DEFAULT_DATE_INIT);
        request.setReceiverDigitalAddress(DEFAULT_STRING_INIT);
        request.setMessageText(DEFAULT_STRING_INIT);
        request.channel(SMS);
        return request;
    }
}
