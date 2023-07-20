package it.pagopa.pn.ec.cartaceo.rest;


import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.service.CartaceoService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.api.PaperMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
public class PaperMessagesApiController implements PaperMessagesApi {


    private final CartaceoService cartaceoService;

    private final StatusPullService paperService;

    public PaperMessagesApiController(CartaceoService cartaceoService, StatusPullService paperService) {
        this.cartaceoService = cartaceoService;
        this.paperService = paperService;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendPaperEngageRequest(String requestIdx, String xPagopaExtchCxId,
                                                             Mono<PaperEngageRequest> paperEngageRequest, ServerWebExchange exchange) {
        log.info(STARTING_PROCESS_ON_LABEL, SEND_PAPER_ENGAGE_REQUEST, requestIdx);
        return paperEngageRequest.flatMap(request ->
                        cartaceoService.presaInCarico(CartaceoPresaInCaricoInfo.builder()
                                .requestIdx(requestIdx)
                                .xPagopaExtchCxId(xPagopaExtchCxId)
                                .paperEngageRequest(request)
                                .build()))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, SEND_PAPER_ENGAGE_REQUEST, requestIdx))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, requestIdx, SEND_PAPER_ENGAGE_REQUEST, throwable, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK));
    }

    @Override
    public Mono<ResponseEntity<PaperProgressStatusEvent>> getPaperEngageProgresses(String requestIdx, String xPagopaExtchCxId,
                                                                                   ServerWebExchange exchange) {
        log.info(STARTING_PROCESS_ON_LABEL, GET_PAPER_ENGAGE_PROGRESSES, requestIdx);
        return paperService.paperPullService(requestIdx, xPagopaExtchCxId)
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, GET_PAPER_ENGAGE_PROGRESSES, requestIdx))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, requestIdx, GET_PAPER_ENGAGE_PROGRESSES, throwable, throwable.getMessage()))
                .map(ResponseEntity::ok);
    }
}
