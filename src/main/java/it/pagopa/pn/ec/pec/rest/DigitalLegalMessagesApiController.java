package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_PEC;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DigitalLegalMessagesApiController implements DigitalLegalMessagesApi {

    private final PecService pecService;

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalLegalMessage(String requestIdx, String xPagopaExtchCxId, Mono<DigitalNotificationRequest> digitalNotificationRequest, ServerWebExchange exchange) {
//        return DigitalLegalMessagesApi.super.sendDigitalLegalMessage(requestIdx, xPagopaExtchCxId, digitalNotificationRequest, exchange);
        return digitalNotificationRequest.flatMap(request -> pecService.presaInCarico(new PecPresaInCaricoInfo(requestIdx,
                        xPagopaExtchCxId,
                        INVIO_PEC,
                        request)))
                .then(Mono.just(new ResponseEntity<>(OK)));
    }

//    public Mono<ResponseEntity<Void>> sendDigitallegalMessage(String requestidx, String xPagopaExtchCxId,
//                                                              Mono<DigitalNotificationRequest> digitalNotificationRequest,
//                                                              final ServerWebExchange exchange) {
//        return digitalNotificationRequest.flatMap(request -> pecService.presaInCarico(new PecPresaInCaricoInfo(requestidx,
//                                                                                                               xPagopaExtchCxId,
//                                                                                                               INVIO_PEC,
//                                                                                                               request)))
//                .then(Mono.just(new ResponseEntity<>(OK)));
//    }
}
