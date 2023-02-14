package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;


@RestController
@Slf4j
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    private final SmsService smsService;

    private final EmailService emailService;

    public DigitalCourtesyMessagesApiController(SmsService smsService, EmailService emailService) {
        this.smsService = smsService;
        this.emailService = emailService;
    }

    @Override
    public Mono<ResponseEntity<Flux<CourtesyMessageProgressEvent>>> getCourtesyShortMessageStatus(String requestIdx,
                                                                                                  String xPagopaExtchCxId, ServerWebExchange exchange) {
        return DigitalCourtesyMessagesApi.super.getCourtesyShortMessageStatus(requestIdx, xPagopaExtchCxId, exchange);
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, String xPagopaExtchCxId,
                                                               Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest,
                                                               final ServerWebExchange exchange) {
        return digitalCourtesySmsRequest.doOnNext(request -> log.info("<-- Start presa in carico -->"))
                                        .flatMap(request -> smsService.presaInCarico(new SmsPresaInCaricoInfo(requestIdx,
                                                                                                              xPagopaExtchCxId,
                                                                                                              request)))
                                        .thenReturn(new ResponseEntity<>(OK));
    }



    /*
        Gli endpoint di SMS ed EMAIL sono state accorpati nello stesso tag OpenApi.
        Ciò ha generato un'interfaccia Java comune e dato che all'interno dello stesso contesto Spring
         non possono coesistere due @RequestController che espongono lo stesso endpoint abbiamo dovuto implementare le API nello stesso controller.
     */

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalCourtesyMessage(String requestIdx, String xPagopaExtchCxId,
                                                                 Mono<DigitalCourtesyMailRequest>  digitalCourtesyMailRequest,
                                                                 final ServerWebExchange exchange){

        return digitalCourtesyMailRequest.doOnNext(request -> log.info("<-- Start presa in email -->"))
                .flatMap(request -> emailService.presaInCarico(new EmailPresaInCaricoInfo(requestIdx,
                        xPagopaExtchCxId,
                        request)))
                .then(Mono.just(new ResponseEntity<>(OK)));
    }
}
