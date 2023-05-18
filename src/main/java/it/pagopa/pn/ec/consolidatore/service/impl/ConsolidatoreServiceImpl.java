package it.pagopa.pn.ec.consolidatore.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.consolidatore.ConsolidatoreEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.consolidatore.exception.SyntaxException;
import it.pagopa.pn.ec.consolidatore.service.ConsolidatoreService;
import it.pagopa.pn.ec.consolidatore.utils.ContentTypes;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.function.Function;

@Service
@Slf4j
public class ConsolidatoreServiceImpl implements ConsolidatoreService {

    @Autowired
    private FileCall fileCall;
    @Autowired
    private ConsolidatoreEndpointProperties consolidatoreEndpointProperties;
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";
    private static final Integer TRACE_ID_LENGTH = 40;


    public Mono<PreLoadResponseData> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, Mono<PreLoadRequestData> attachments) {
        log.info("<-- START PRESIGNED UPLOAD REQUEST --> Client ID : {}", xPagopaExtchServiceId);
        return checkHeaders(xPagopaExtchServiceId, xApiKey)
                .then(attachments.map(PreLoadRequestData::getPreloads))
                .flatMapMany(Flux::fromIterable)
                .transform(checkFields)
                .cast(PreLoadRequest.class)
                .flatMap(preLoadRequest ->
                {
                    var fileCreationRequest = new FileCreationRequest();
                    fileCreationRequest.setContentType(preLoadRequest.getContentType());
                    fileCreationRequest.setStatus("");
                    fileCreationRequest.setDocumentType(DOC_TYPE);

                    String xTraceId = RandomStringUtils.randomAlphanumeric(TRACE_ID_LENGTH);

                    return fileCall.postFile(xPagopaExtchServiceId, "", preLoadRequest.getSha256(),  xTraceId, fileCreationRequest)
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
        return checkHeaders(xPagopaExtchServiceId, xApiKey)
                .then(fileCall.getFile(fileKey, xPagopaExtchServiceId, xApiKey, RandomStringUtils.randomAlphanumeric(TRACE_ID_LENGTH)));
    }

    private Mono<Void> checkHeaders(String xPagopaExtchServiceId, String xApiKey) {
        if (StringUtils.isBlank(xPagopaExtchServiceId))
            return Mono.error(new SyntaxException(consolidatoreEndpointProperties.clientHeaderName()));
        else return Mono.empty();
    }

    private final Function<Flux<PreLoadRequest>, Flux<Object>> checkFields =
            f -> f.handle((preLoadRequest, synchronousSink) ->
            {
                String contentType = preLoadRequest.getContentType();
                if (StringUtils.isBlank(preLoadRequest.getContentType()))
                    synchronousSink.error(new SyntaxException("contentType"));
                else if (StringUtils.isBlank(preLoadRequest.getPreloadIdx()))
                    synchronousSink.error(new SyntaxException("preloadIdX"));
                else if (StringUtils.isBlank(preLoadRequest.getSha256()))
                    synchronousSink.error(new SyntaxException("sha256"));
                else if (!ContentTypes.CONTENT_TYPE_LIST.contains(contentType))
                    synchronousSink.error(new SemanticException("contentType"));
                else synchronousSink.next(preLoadRequest);
            });

}
