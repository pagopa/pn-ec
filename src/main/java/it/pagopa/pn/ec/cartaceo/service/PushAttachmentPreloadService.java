package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.FilesEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class PushAttachmentPreloadService {

    @Autowired
    private WebClient ssWebClient;
    @Autowired
    private FileCall fileCall;
    @Autowired
    private FilesEndpointProperties filesEndpointProperties;

    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";
    private static final String DOC_STATUS = "SAVED";


    public Mono<PreLoadResponseSchema> presignedUploadRequest(Mono<PreLoadRequestSchema> attachments) {
        return attachments.map(PreLoadRequestSchema::getPreloads)
                .flatMapMany(Flux::fromIterable)
                .flatMap(preLoadRequest ->
                {
                    var fileCreationRequest = new FileCreationRequest();
                    fileCreationRequest.setContentType(preLoadRequest.getContentType());
                    fileCreationRequest.setStatus(DOC_STATUS);
                    fileCreationRequest.setDocumentType(DOC_TYPE);
                    return fileCall.postFile(fileCreationRequest)
                            .flux()
                            .map(fileCreationResponse ->
                            {
                                var preLoadResponse = new PreLoadResponse();
                                preLoadResponse.setKey(fileCreationResponse.getKey());
                                preLoadResponse.setSecret(fileCreationResponse.getSecret());
                                preLoadResponse.setUrl(fileCreationResponse.getUploadUrl());
                                preLoadResponse.setHttpMethod(PreLoadResponse.HttpMethodEnum.POST);
                                preLoadResponse.setPreloadIdx(preLoadRequest.getPreloadIdx());
                                return preLoadResponse;
                            });
                }).collectList()
                .map(list ->
                {
                    var preLoadResponse = new PreLoadResponseSchema();
                    for (var r : list) {
                        preLoadResponse.getPreloads().add((PreLoadResponse) r);
                    }
                    return preLoadResponse;
                });
    }

}
