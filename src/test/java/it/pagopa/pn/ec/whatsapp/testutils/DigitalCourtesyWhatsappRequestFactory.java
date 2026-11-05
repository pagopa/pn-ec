package it.pagopa.pn.ec.whatsapp.testutils;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest;
import it.pagopa.pn.ec.rest.v1.dto.WhatsappTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest.ChannelEnum.WHATSAPP;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;

public class DigitalCourtesyWhatsappRequestFactory {

    private DigitalCourtesyWhatsappRequestFactory() {
    }

    public static WhatsappTemplate createTemplate() {
        return new WhatsappTemplate()
                .name("send_avviso_notifica_digitale")
                .language("it")
                .bodyParameters(List.of("Comune di Test", "ABCD-EFGH-IJKL-202610-M-1"));
    }

    public static DigitalCourtesyWhatsappRequest createWhatsappRequest() {
        var request = new DigitalCourtesyWhatsappRequest();
        request.setRequestId(DEFAULT_REQUEST_IDX);
        request.eventType("COURTESY");
        request.setClientRequestTimeStamp(OffsetDateTime.now());
        request.setQos(INTERACTIVE);
        request.setReceiverDigitalAddress("+393890091180");
        request.setMessageText("Hai ricevuto una notifica SEND da Comune di Test con Codice IUN ABCD-EFGH-IJKL-202610-M-1.");
        request.setWhatsappTemplate(createTemplate());
        request.channel(WHATSAPP);
        return request;
    }
}
