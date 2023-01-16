package it.pagopa.pn.ec.rest;

import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    private final AuthService authService;

    public DigitalCourtesyMessagesApiController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, String xPagopaExtchCxId,
                                                               Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest,
                                                               final ServerWebExchange exchange) {
        authService.checkIdClient(xPagopaExtchCxId);
        // TODO:sendCourtesyShortMessage -> Change HttpStatus.NOT_IMPLEMENTED
        return digitalCourtesySmsRequest.map(req -> new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED));
    }
}
