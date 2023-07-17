package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.LegalMessageSentDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.OK;


@Slf4j
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
        log.info(STARTING_PROCESS_LABEL, SEND_DIGITAL_LEGAL_MESSAGE);
        return digitalNotificationRequest
                .flatMap(request -> pecService.presaInCarico(PecPresaInCaricoInfo.builder()
                        .requestIdx(requestIdx)
                        .xPagopaExtchCxId(xPagopaExtchCxId)
                        .digitalNotificationRequest(request)
                        .build()))
                .doOnSuccess(result -> log.info(ENDING_PROCESS_LABEL, SEND_DIGITAL_LEGAL_MESSAGE))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_WITH_ERROR_LABEL, SEND_DIGITAL_LEGAL_MESSAGE, throwable, throwable.getMessage()))
                .thenReturn(new ResponseEntity<>(OK));
    }

    @Override
    public Mono<ResponseEntity<LegalMessageSentDetails>> getDigitalLegalMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                      ServerWebExchange exchange) {
        log.info(STARTING_PROCESS_LABEL, GET_DIGITAL_LEGAL_MESSAGE_STATUS);
        return statusPullService.pecPullService(requestIdx, xPagopaExtchCxId)
                .doOnSuccess(result -> log.info(ENDING_PROCESS_LABEL, GET_DIGITAL_LEGAL_MESSAGE_STATUS))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_WITH_ERROR_LABEL, GET_DIGITAL_LEGAL_MESSAGE_STATUS, throwable, throwable.getMessage()))
                .map(ResponseEntity::ok);
    }

}
