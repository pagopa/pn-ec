package it.pagopa.pn.ec.commons.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class HealthCheckApiController {

	@GetMapping
    public Mono<ResponseEntity<Void>> status(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok().build());
    }
}
