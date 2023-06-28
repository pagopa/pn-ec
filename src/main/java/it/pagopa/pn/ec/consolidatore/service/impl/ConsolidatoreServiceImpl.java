package it.pagopa.pn.ec.consolidatore.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogEvent;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.consolidatore.exception.SyntaxException;
import it.pagopa.pn.ec.consolidatore.service.ConsolidatoreService;
import it.pagopa.pn.ec.consolidatore.utils.ContentTypes;
import it.pagopa.pn.ec.rest.v1.dto.*;
import jdk.dynalink.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.*;
import static it.pagopa.pn.ec.consolidatore.utils.LogUtils.*;

@Service
@Slf4j
public class ConsolidatoreServiceImpl implements ConsolidatoreService {

    @Autowired
    private FileCall fileCall;
    @Autowired
    private ConsolidatoreEndpointProperties consolidatoreEndpointProperties;
    @Autowired
    private AuthService authService;
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";
    private static final Integer TRACE_ID_LENGTH = 40;


    public Mono<PreLoadResponseData> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, Mono<PreLoadRequestData> attachments) {
        log.info("<-- START PRESIGNED UPLOAD REQUEST --> Client ID : {}", xPagopaExtchServiceId);
        return authService.clientAuth(xPagopaExtchServiceId)
                .flatMap(clientConfiguration -> {
                    if (!clientConfiguration.getApiKey().equals(xApiKey)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_API_KEY));
                    }
                    return Mono.just(clientConfiguration);
                })
                .then(checkHeaders(xPagopaExtchServiceId, xApiKey))
                .then(attachments.map(PreLoadRequestData::getPreloads))
                .flatMapMany(Flux::fromIterable)
                .transform(checkSyntaxErrors())
                .cast(PreLoadRequest.class)
                .handle((preLoadRequest, synchronousSink) ->
                {
                    String contentType = preLoadRequest.getContentType();
                    var requestId=preLoadRequest.getPreloadIdx();

                    if (!ContentTypes.CONTENT_TYPE_LIST.contains(contentType)) {
                        var consAuditLogError = ConsAuditLogError.builder().error(ERR_CONS_BAD_CONTENT_TYPE.getValue()).description("ContentType is not valid.").requestId(requestId).build();
                        synchronousSink.error(new SemanticException().errorList(List.of("ContentType is not valid.")).auditLogErrorList(List.of(consAuditLogError)));
                    } else synchronousSink.next(preLoadRequest);
                })
                .cast(PreLoadRequest.class)
                .flatMap(preLoadRequest ->
                {
                    var fileCreationRequest = new FileCreationRequest();
                    fileCreationRequest.setContentType(preLoadRequest.getContentType());
                    fileCreationRequest.setStatus("");
                    fileCreationRequest.setDocumentType(DOC_TYPE);

                    String xTraceId = RandomStringUtils.randomAlphanumeric(TRACE_ID_LENGTH);

                    return fileCall.postFile(xPagopaExtchServiceId, xApiKey, preLoadRequest.getSha256(), xTraceId, fileCreationRequest)
                            .doOnError(ConnectException.class, e -> log.error("* FATAL * presignedUploadRequest - {}, {}", e, e.getMessage()))
                            .flux()
                            .map(fileCreationResponse ->
                            {
                                var preLoadResponse = new PreLoadResponse();
                                preLoadResponse.setKey(fileCreationResponse.getKey());
                                preLoadResponse.setSecret(fileCreationResponse.getSecret());
                                preLoadResponse.setUrl(fileCreationResponse.getUploadUrl());

                                var uploadMethod = fileCreationResponse.getUploadMethod();
                                if (uploadMethod == null) {
                                    preLoadResponse.setHttpMethod(PreLoadResponse.HttpMethodEnum.PUT);
                                } else
                                    preLoadResponse.setHttpMethod(PreLoadResponse.HttpMethodEnum.fromValue(uploadMethod.name()));

                                preLoadResponse.setPreloadIdx(preLoadRequest.getPreloadIdx());
                                return preLoadResponse;
                            });
                })
                .collectList()
                .map(preLoadResponseList ->
                {
                    var preLoadResponseSchema = new PreLoadResponseData();
                    for (var preLoadResponse : preLoadResponseList) {
                        preLoadResponseSchema.getPreloads().add(preLoadResponse);
                    }
                    return preLoadResponseSchema;
                });
    }


    public Mono<FileDownloadResponse> getFile(String fileKey, String xPagopaExtchServiceId
            , String xApiKey) {
        log.info("<-- START GET FILE --> Client ID : {}", xPagopaExtchServiceId);
        return authService.clientAuth(xPagopaExtchServiceId)
                .flatMap(clientConfiguration -> {
                    if (!clientConfiguration.getApiKey().equals(xApiKey)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid API key"));
                    }
                    return Mono.just(clientConfiguration);
                })
                .then(checkHeaders(xPagopaExtchServiceId, xApiKey))
                .then(fileCall.getFile(fileKey, xPagopaExtchServiceId, xApiKey, RandomStringUtils.randomAlphanumeric(TRACE_ID_LENGTH)))
                .doOnError(ConnectException.class, e -> log.error("* FATAL * getFile - {}, {}", e, e.getMessage()));
    }

    private Mono<Void> checkHeaders(String xPagopaExtchServiceId, String xApiKey) {
        if (StringUtils.isBlank(xPagopaExtchServiceId)) {
            var consAuditLogError = ConsAuditLogError.builder().error(ERR_CONS_BAD_SERVICE_ID.getValue()).description("ServiceID is blank or null").build();
            return Mono.error(new SyntaxException().errorList(List.of("Field xPagopaExtchServiceId is required")).auditLogErrorList(List.of(consAuditLogError)));
        } else return Mono.empty();
    }

    private Function<Flux<PreLoadRequest>, Flux<PreLoadRequest>> checkSyntaxErrors() {
        return tFlux -> tFlux.handle((preLoadRequest, synchronousSink) ->
        {
            var requestId=preLoadRequest.getPreloadIdx();

            var errorList = new ArrayList<String>();
            var auditLogErrorList= new ArrayList<ConsAuditLogError>();

            if (StringUtils.isBlank(preLoadRequest.getContentType())) {
                auditLogErrorList.add(ConsAuditLogError.builder().requestId(requestId).error(ERR_CONS_BAD_CONTENT_TYPE.getValue()).description("Field contentType is required").build());
                errorList.add("Field contentType is required");
            } else if (StringUtils.isBlank(preLoadRequest.getPreloadIdx())) {
                auditLogErrorList.add(ConsAuditLogError.builder().requestId(requestId).error(ERR_CONS_BAD_PRELOAD_IDX.getValue()).description("Field preloadIdX is required").build());
                errorList.add("Field preloadIdX is required");
            } else if (StringUtils.isBlank(preLoadRequest.getSha256())) {
                auditLogErrorList.add(ConsAuditLogError.builder().requestId(requestId).error(ERR_CONS_BAD_SHA_256.getValue()).description("Field sha256 is required").build());
                errorList.add("Field sha256 is required");
            }

            if (errorList.isEmpty()) {
                synchronousSink.next(preLoadRequest);
            } else
                synchronousSink.error(new SyntaxException().errorList(errorList).auditLogErrorList(auditLogErrorList));
        });
    }
}
