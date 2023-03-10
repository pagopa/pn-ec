package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static it.pagopa.pn.ec.repositorymanager.utils.RequestMapper.createRequestFromPersonalAndMetadata;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {


    private final RequestPersonalService requestPersonalService;
    private final RequestMetadataService requestMetadataService;

    public RequestServiceImpl(RequestPersonalService requestPersonalService, RequestMetadataService requestMetadataService) {
        this.requestPersonalService = requestPersonalService;
        this.requestMetadataService = requestMetadataService;
    }

    @Override
    public Mono<Request> getRequest(String requestIdx) {
        return Mono.zip(requestPersonalService.getRequestPersonal(requestIdx), requestMetadataService.getRequestMetadata(requestIdx))
                   .map(objects -> {
                       RequestPersonal retrievedRequestPersonal = objects.getT1();
                       RequestMetadata retrievedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                   });
    }

    @Override
    public Mono<Request> insertRequest(Request request) {

        String requestId = request.getRequestId();
        String clientId = request.getXPagopaExtchCxId();

        RequestPersonal requestPersonal = request.getRequestPersonal();
        requestPersonal.setRequestId(requestId);
        requestPersonal.setXPagopaExtchCxId(clientId);
        requestPersonal.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
        requestPersonal.setRequestTimestamp(OffsetDateTime.now());

        RequestMetadata requestMetadata = request.getRequestMetadata();
        requestMetadata.setRequestId(requestId);
        requestMetadata.setXPagopaExtchCxId(clientId);
        requestMetadata.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
        requestMetadata.setRequestTimestamp(OffsetDateTime.now());

        if ((requestPersonal.getDigitalRequestPersonal() != null && requestMetadata.getPaperRequestMetadata() != null) ||
            (requestPersonal.getPaperRequestPersonal() != null && requestMetadata.getDigitalRequestMetadata() != null)) {
            throw new RepositoryManagerException.RequestMalformedException("Incompatibilità dati sensibili con metadata");
        }

        return Mono.zip(requestPersonalService.insertRequestPersonal(requestPersonal),
                        requestMetadataService.insertRequestMetadata(requestMetadata)).map(objects -> {
            RequestPersonal insertedRequestPersonal = objects.getT1();
            RequestMetadata insertedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(insertedRequestPersonal, insertedRequestMetadata);
        });
    }

    @Override
    public Mono<Request> patchRequest(String requestIdx, Patch patch) {
        return Mono.zip(requestPersonalService.getRequestPersonal(requestIdx),
                        requestMetadataService.patchRequestMetadata(requestIdx, patch)).map(objects -> {
            RequestPersonal retrievedRequestPersonal = objects.getT1();
            RequestMetadata updatedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, updatedRequestMetadata);
        });
    }

    @Override
    public Mono<Request> deleteRequest(String requestIdx) {
        return Mono.zip(requestPersonalService.deleteRequestPersonal(requestIdx), requestMetadataService.deleteRequestMetadata(requestIdx))
                   .map(objects -> {
                       RequestPersonal deletedRequestPersonal = objects.getT1();
                       RequestMetadata deletedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(deletedRequestPersonal, deletedRequestMetadata);
                   });
    }

    @Override
    public Mono<Request> getRequestByMessageId(String messageId) {
        return requestMetadataService.getRequestMetadataByMessageId(messageId)
                                     .zipWhen(requestMetadata -> requestPersonalService.getRequestPersonal(requestMetadata.getRequestId()))
                                     .map(objects -> {
                                         RequestMetadata retrievedRequestMetadata = objects.getT1();
                                         RequestPersonal retrievedRequestPersonal = objects.getT2();
                                         return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                                     });
    }

    @Override
    public Mono<Request> setMessageIdInRequestMetadata(String requestIdx) {
        return requestMetadataService.setMessageIdInRequestMetadata(requestIdx)
                                     .zipWhen(requestMetadata -> requestPersonalService.getRequestPersonal(requestMetadata.getRequestId()))
                                     .map(objects -> {
                                         RequestMetadata retrievedRequestMetadata = objects.getT1();
                                         RequestPersonal retrievedRequestPersonal = objects.getT2();
                                         return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                                     });
    }
}
