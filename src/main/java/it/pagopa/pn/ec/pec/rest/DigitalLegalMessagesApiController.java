package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static org.springframework.http.HttpStatus.OK;


@CustomLog
@RestController
public class DigitalLegalMessagesApiController implements DigitalLegalMessagesApi {

    private final PecService pecService;
    private final StatusPullService statusPullService;

    public DigitalLegalMessagesApiController(PecService pecService, StatusPullService statusPullService) {
        this.pecService = pecService;
        this.statusPullService = statusPullService;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalLegalMessage(String requestIdx, String xPagopaExtchCxId,
                                                              Mono<DigitalNotificationRequest> digitalNotificationRequest,
                                                              final ServerWebExchange exchange) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(SEND_DIGITAL_LEGAL_MESSAGE);
        return MDCUtils.addMDCToContextAndExecute(digitalNotificationRequest
                .flatMap(request -> pecService.presaInCarico(PecPresaInCaricoInfo.builder()
                        .requestIdx(requestIdx)
                        .xPagopaExtchCxId(xPagopaExtchCxId)
                        .digitalNotificationRequest(request)
                        .build()))
                .doOnSuccess(result -> log.logEndingProcess(SEND_DIGITAL_LEGAL_MESSAGE))
                .doOnError(throwable -> log.logEndingProcess(SEND_DIGITAL_LEGAL_MESSAGE, false, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK)));
    }

    @Override
    public Mono<ResponseEntity<LegalMessageSentDetails>> getDigitalLegalMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                      ServerWebExchange exchange) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(GET_DIGITAL_LEGAL_MESSAGE_STATUS);
        return MDCUtils.addMDCToContextAndExecute(statusPullService.pecPullService(requestIdx, xPagopaExtchCxId)
                .doOnSuccess(result -> log.logEndingProcess(GET_DIGITAL_LEGAL_MESSAGE_STATUS))
                .doOnError(throwable -> log.logEndingProcess(GET_DIGITAL_LEGAL_MESSAGE_STATUS, false, throwable.getMessage()))
                .map(ResponseEntity::ok));
    }

}
