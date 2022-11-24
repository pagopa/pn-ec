package it.pagopa.pn.template.rest;

import it.pagopa.pn.template.rest.v1.api.TemplateSampleApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class TemplateSampleApiController implements TemplateSampleApi {

    @Override
    public Mono<ResponseEntity<Map<String, List<String>>>> getHttpHeadersMap(ServerWebExchange exchange) {


        return Mono.fromSupplier(() ->{
            log.debug("Start getHttpHeadersMap");
            Map<String, List<String>> headers = new HashMap<>();
            exchange.getRequest().getHeaders().forEach((k, v) -> headers.put(k, v));
            return ResponseEntity.ok(headers);
        });

    }
}
