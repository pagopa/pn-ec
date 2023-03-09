package it.pagopa.pn.ec.consolidatore.controller;

import it.pagopa.pn.ec.consolidatore.service.PushAttachmentPreloadService;
import it.pagopa.pn.ec.rest.v1.api.ConsolidatoreApi;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequestData;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadResponseData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ConsolidatoreApiController implements ConsolidatoreApi {

    @Autowired
    private PushAttachmentPreloadService service;

    @Override
    public Mono<ResponseEntity<PreLoadResponseData>> presignedUploadRequest(Mono<PreLoadRequestData> preLoadRequestData, ServerWebExchange exchange) {
        return service.presignedUploadRequest(preLoadRequestData).map(ResponseEntity::ok);
    }

}
