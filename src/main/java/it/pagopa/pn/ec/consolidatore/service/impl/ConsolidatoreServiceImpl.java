package it.pagopa.pn.ec.consolidatore.service.impl;

import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.consolidatore.service.ConsolidatoreService;
import it.pagopa.pn.ec.consolidatore.utils.ContentTypes;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ConsolidatoreServiceImpl implements ConsolidatoreService {

    @Autowired
    private FileCall fileCall;
    private static final String DOC_TYPE = "PN_EXTERNAL_LEGAL_FACTS";


    public Mono<PreLoadResponseData> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, Mono<PreLoadRequestData> attachments) {
        log.info("<-- START PRESIGNED UPLOAD REQUEST --> Client ID : {}", xPagopaExtchServiceId);
        return attachments.map(PreLoadRequestData::getPreloads)
                .flatMapMany(Flux::fromIterable)
                .handle(((preLoadRequest, synchronousSink) -> {
                    String contentType = preLoadRequest.getContentType();
                    if (!ContentTypes.CONTENT_TYPE_LIST.contains(contentType))
                        synchronousSink.error(new SemanticException("contentType"));
                    else synchronousSink.next(preLoadRequest);
                }))
                .flatMap(object ->
                {
                    var preLoadRequest = (PreLoadRequest) object;
                    var fileCreationRequest = new FileCreationRequest();
                    fileCreationRequest.setChecksumValue(preLoadRequest.getSha256());
                    fileCreationRequest.setContentType(preLoadRequest.getContentType());
                    fileCreationRequest.setStatus("");
                    fileCreationRequest.setDocumentType(DOC_TYPE);
                    return fileCall.postFile(xPagopaExtchServiceId, xApiKey, fileCreationRequest)
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
        return fileCall.getFile(fileKey, xPagopaExtchServiceId, xApiKey);
    }

}
