package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.impl.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_MAIL;
import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static org.springframework.http.HttpStatus.OK;


@Slf4j
@RestController
@RequiredArgsConstructor
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    private final SmsService smsService;

    private final EmailService service;

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
                                                                 Mono<DigitalCourtesyMailRequest>  digitalCourtesyMailRequest,
                                                                 final ServerWebExchange exchange){

        return digitalCourtesyMailRequest.flatMap(request -> service.presaInCarico(new EmailPresaInCaricoInfo(requestIdx,
                        xPagopaExtchCxId,
                        INVIO_MAIL,
                        request)))
                .then(Mono.just(new ResponseEntity<>(OK)));
    }
}
