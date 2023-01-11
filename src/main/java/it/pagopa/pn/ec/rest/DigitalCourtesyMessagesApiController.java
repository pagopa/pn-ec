package it.pagopa.pn.ec.rest;

import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class DigitalCourtesyMessagesApiController implements DigitalCourtesyMessagesApi {

    @Override
    public Mono<ResponseEntity<Void>> sendCourtesyShortMessage(String requestIdx, Mono<DigitalCourtesySmsRequest> digitalCourtesySmsRequest, final ServerWebExchange exchange) {

    }
}
