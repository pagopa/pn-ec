package it.pagopa.pn.ec.cartaceo.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.cartaceo.service.PaperEngageProgressesService;
import it.pagopa.pn.ec.rest.v1.consolidatore.api.PaperProgressesApi;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@CustomLog
@RestController
public class PaperProgressesApiController implements PaperProgressesApi {

    private final PaperEngageProgressesService paperEngageProgressesService;

    public PaperProgressesApiController(PaperEngageProgressesService paperEngageProgressesService) {
        this.paperEngageProgressesService = paperEngageProgressesService;
    }

    @Override
    public Mono<ResponseEntity<PaperDeliveryProgressesResponse>> getPaperEngageProgresses(String requestIdx, String xPagopaExtchCxId,
                                                                                            ServerWebExchange exchange) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId);
        log.logStartingProcess(GET_PAPER_ENGAGE_PROGRESSES);
        return MDCUtils.addMDCToContextAndExecute(paperEngageProgressesService.getPaperEngageProgresses(requestIdx, xPagopaExtchCxId)
                .doOnSuccess(result -> log.logEndingProcess(GET_PAPER_ENGAGE_PROGRESSES))
                .doOnError(throwable -> log.logEndingProcess(GET_PAPER_ENGAGE_PROGRESSES, false, throwable.getMessage(), throwable))
                .map(ResponseEntity::ok));
    }
}
