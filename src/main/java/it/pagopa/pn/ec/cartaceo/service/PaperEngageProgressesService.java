package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperDeliveryProgressesResponse;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@Service
@CustomLog
public class PaperEngageProgressesService {

    private final AuthService authService;
    private final PaperMessageCall paperMessageCall;

    public PaperEngageProgressesService(AuthService authService, PaperMessageCall paperMessageCall) {
        this.authService = authService;
        this.paperMessageCall = paperMessageCall;
    }

    public Mono<PaperDeliveryProgressesResponse> getPaperEngageProgresses(String requestIdx, String xPagopaExtchCxId) {
        String concatRequestId = concatRequestId(xPagopaExtchCxId, requestIdx);
        log.info(INVOKING_OPERATION_LABEL_WITH_ARGS, PAPER_ENGAGE_PROGRESSES_SERVICE, concatRequestId);

        return authService.clientAuth(xPagopaExtchCxId)
                          .then(Mono.defer(() -> paperMessageCall.getProgress(requestIdx)))
                          .doOnNext(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL,
                                                       concatRequestId,
                                                       PAPER_ENGAGE_PROGRESSES_SERVICE,
                                                       result));
    }
}
