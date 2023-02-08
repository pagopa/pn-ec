package it.pagopa.pn.ec.pec.rest;

import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.pec.service.impl.PecService;
import it.pagopa.pn.ec.rest.v1.api.DigitalLegalMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
public class DigitalLegalMessagesApiController implements DigitalLegalMessagesApi {

    private final PecService pecService;

    public DigitalLegalMessagesApiController(PecService pecService) {
        this.pecService = pecService;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalLegalMessage(String requestIdx, String xPagopaExtchCxId, Mono<DigitalNotificationRequest> digitalNotificationRequest, final ServerWebExchange exchange) {
        return digitalNotificationRequest.doOnNext(request -> log.info("<-- Start presa in carico -->"))
                                         .flatMap(request -> pecService.presaInCarico(new PecPresaInCaricoInfo(requestIdx,
                                                                                                               xPagopaExtchCxId,
                                                                                                               request)))
                                         .thenReturn(new ResponseEntity<>(OK));
    }


}
