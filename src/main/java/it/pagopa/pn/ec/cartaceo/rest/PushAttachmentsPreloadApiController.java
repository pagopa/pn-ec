package it.pagopa.pn.ec.cartaceo.rest;

import it.pagopa.pn.ec.cartaceo.service.PushAttachmentPreloadService;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.rest.v1.api.PushAttachmentsPreloadApi;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequest;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequestSchema;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadResponseSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class PushAttachmentsPreloadApiController implements PushAttachmentsPreloadApi {
    @Autowired
    private PushAttachmentPreloadService service;


    @Override
    public Mono<ResponseEntity<PreLoadResponseSchema>> presignedUploadRequest(Mono<PreLoadRequestSchema> preLoadRequestSchema, ServerWebExchange exchange) {
        return service.presignedUploadRequest(preLoadRequestSchema).map(ResponseEntity::ok);
    }

}
