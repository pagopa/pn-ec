package it.pagopa.pn.ec.testutils.factory;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;

import java.util.Date;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.ChannelEnum.SMS;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;

/**
 * Classe con metodi statici factory che creano gli oggetti request usati nei vari endpoint di EC.
 * Utili per i test sui RestController
 */
public class EcRequestObjectFactory {

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

    public static DigitalNotificationRequest getDigitalNotificationRequest() {
        var requestPec = new DigitalNotificationRequest();
        requestPec.setRequestId(DEFAULT_STRING_INIT);
        requestPec.setEventType(DEFAULT_STRING_INIT);
        requestPec.setQos(BATCH);
        requestPec.setClientRequestTimeStamp(DEFAULT_DATE_INIT);
        requestPec.setReceiverDigitalAddress(DEFAULT_STRING_INIT);
        requestPec.setMessageText(DEFAULT_STRING_INIT);
        requestPec.channel(PEC);
        requestPec.setSubjectText(DEFAULT_STRING_INIT);
        requestPec.setMessageContentType(PLAIN);

        return requestPec;
    }
}
