package it.pagopa.pn.ec.sms.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
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
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static org.springframework.http.HttpStatus.OK;

@RestController
@CustomLog
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
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(GET_COURTESY_SHORT_MESSAGE_STATUS);
        return MDCUtils.addMDCToContextAndExecute(statusPullService.digitalPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.sms())
                .doOnSuccess(result -> log.logEndingProcess(GET_COURTESY_SHORT_MESSAGE_STATUS))
                .doOnError(throwable -> log.logEndingProcess(GET_COURTESY_SHORT_MESSAGE_STATUS, false, throwable.getMessage()))
                .map(ResponseEntity::ok));
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, String xPagopaExtchCxId,
                                                               Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest,
                                                               final ServerWebExchange exchange) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(SEND_COURTESY_SHORT_MESSAGE);
        return MDCUtils.addMDCToContextAndExecute(digitalCourtesySmsRequest.flatMap(request ->
                        smsService.presaInCarico(SmsPresaInCaricoInfo.builder()
                                .requestIdx(requestIdx)
                                .xPagopaExtchCxId(xPagopaExtchCxId)
                                .digitalCourtesySmsRequest(request)
                                .build()))
                .doOnSuccess(result -> log.logEndingProcess(SEND_COURTESY_SHORT_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(SEND_COURTESY_SHORT_MESSAGE, false, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK)));
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
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(SEND_DIGITAL_COURTESY_MESSAGE);
        return MDCUtils.addMDCToContextAndExecute(digitalCourtesyMailRequest.flatMap(request ->
                        emailService.presaInCarico(EmailPresaInCaricoInfo.builder()
                                .requestIdx(requestIdx)
                                .xPagopaExtchCxId(xPagopaExtchCxId)
                                .digitalCourtesyMailRequest(request)
                                .build()))
                .doOnSuccess(result -> log.logEndingProcess(SEND_DIGITAL_COURTESY_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(SEND_DIGITAL_COURTESY_MESSAGE, false, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK)));
    }

    @Override
    public Mono<ResponseEntity<CourtesyMessageProgressEvent>> getDigitalCourtesyMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                              ServerWebExchange exchange) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(GET_DIGITAL_COURTESY_MESSAGE_STATUS);
        return MDCUtils.addMDCToContextAndExecute(statusPullService.digitalPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.email())
                .doOnSuccess(result -> log.logEndingProcess(GET_DIGITAL_COURTESY_MESSAGE_STATUS))
                .doOnError(throwable -> log.logEndingProcess(GET_DIGITAL_COURTESY_MESSAGE_STATUS, false, throwable.getMessage()))
                .map(ResponseEntity::ok));
    }
}
