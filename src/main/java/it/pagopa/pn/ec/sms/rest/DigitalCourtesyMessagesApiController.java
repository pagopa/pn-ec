package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.service.StatusPullService;
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
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
@Slf4j
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    private final SmsService smsService;
    private final EmailService emailService;
    private final StatusPullService statusPullService;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;


    public DigitalCourtesyMessagesApiController(SmsService smsService, EmailService emailService, StatusPullService statusPullService,
                                                TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.smsService = smsService;
        this.emailService = emailService;
        this.statusPullService = statusPullService;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    public Mono<ResponseEntity<CourtesyMessageProgressEvent>> getCourtesyShortMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                            ServerWebExchange exchange) {

        return statusPullService.digitalPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.sms())
                                .map(ResponseEntity::ok);

    }

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, String xPagopaExtchCxId,
                                                               Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest,
                                                               final ServerWebExchange exchange) {

        return digitalCourtesySmsRequest
                                        .flatMap(request -> smsService.presaInCarico(SmsPresaInCaricoInfo.builder()
                                                                                                         .requestIdx(requestIdx)
                                                                                                         .xPagopaExtchCxId(xPagopaExtchCxId)
                                                                                                         .digitalCourtesySmsRequest(request)
                                                                                                         .build()))
                                        .thenReturn(new ResponseEntity<>(OK));

    }

    /*
     * Gli endpoint di SMS ed EMAIL sono state accorpati nello stesso tag OpenApi.
     * Ciò ha generato un'interfaccia Java comune e dato che all'interno dello
     * stesso contesto Spring non possono coesistere due @RequestController che
     * espongono lo stesso endpoint abbiamo dovuto implementare le API nello stesso
     * controller.
     */

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalCourtesyMessage(String requestIdx, String xPagopaExtchCxId,
                                                                 Mono<DigitalCourtesyMailRequest> digitalCourtesyMailRequest,
                                                                 final ServerWebExchange exchange) {

        return digitalCourtesyMailRequest
                                         .flatMap(request -> emailService.presaInCarico(EmailPresaInCaricoInfo.builder()
                                                                                                              .requestIdx(requestIdx)
                                                                                                              .xPagopaExtchCxId(
                                                                                                                      xPagopaExtchCxId)
                                                                                                              .digitalCourtesyMailRequest(
                                                                                                                      request)
                                                                                                              .build()))
                                         .thenReturn(new ResponseEntity<>(OK));

    }

    @Override
    public Mono<ResponseEntity<CourtesyMessageProgressEvent>> getDigitalCourtesyMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                              ServerWebExchange exchange) {

        return statusPullService.digitalPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.email())
                                .map(ResponseEntity::ok);
    }
}
