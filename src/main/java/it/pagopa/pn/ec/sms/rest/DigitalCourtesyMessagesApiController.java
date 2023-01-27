package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.impl.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static org.springframework.http.HttpStatus.OK;


@RestController
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    private final SmsService smsService;

    public DigitalCourtesyMessagesApiController(SmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, String xPagopaExtchCxId,
                                                               Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest,
                                                               final ServerWebExchange exchange) {
        return digitalCourtesySmsRequest.flatMap(request -> smsService.presaInCarico(new SmsPresaInCaricoInfo(requestIdx,
                                                                                                              xPagopaExtchCxId,
                                                                                                              INVIO_SMS,
                                                                                                              request)))
                                        .then(Mono.just(new ResponseEntity<>(OK)));
    }

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalCourtesyMessage(String requestIdx, String xPagopaExtchCxId,
                                                                 Mono<DigitalCourtesyMailRequest> digitalCourtesyMailRequest,
                                                                 ServerWebExchange exchange) {
        return digitalCourtesyMailRequest.then(Mono.just(new ResponseEntity<>(OK)));
    }
}
