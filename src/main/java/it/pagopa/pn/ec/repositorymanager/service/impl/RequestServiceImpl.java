package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
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
    public Mono<Request> getRequest(String xPagopaExtchCxId, String requestIdx) {
        return Mono.zip(requestPersonalService.getRequestPersonal(xPagopaExtchCxId, requestIdx), requestMetadataService.getRequestMetadata(xPagopaExtchCxId, requestIdx))
                   .map(objects -> {
                       RequestPersonal retrievedRequestPersonal = objects.getT1();
                       RequestMetadata retrievedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(xPagopaExtchCxId, requestIdx, retrievedRequestPersonal, retrievedRequestMetadata);
                   });
    }

    @Override
    public Mono<Request> insertRequest(String xPagopaExtchCxId, Request request) {

        String requestId = request.getRequestId();

        RequestPersonal requestPersonal = request.getRequestPersonal();
        requestPersonal.setRequestId(requestId);
        requestPersonal.setXPagopaExtchCxId(xPagopaExtchCxId);
        requestPersonal.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
        requestPersonal.setRequestTimestamp(OffsetDateTime.now());

        RequestMetadata requestMetadata = request.getRequestMetadata();
        requestMetadata.setRequestId(requestId);
        requestMetadata.setXPagopaExtchCxId(xPagopaExtchCxId);
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
            return createRequestFromPersonalAndMetadata(xPagopaExtchCxId, requestId, insertedRequestPersonal, insertedRequestMetadata);
        });
    }

    @Override
    public Mono<Request> updateEvents(String xPagopaExtchCxId, String requestIdx, Events events) {
        return Mono.zip(requestPersonalService.getRequestPersonal(xPagopaExtchCxId, requestIdx),
                        requestMetadataService.updateEventsMetadata(xPagopaExtchCxId, requestIdx, events)).map(objects -> {
            RequestPersonal retrievedRequestPersonal = objects.getT1();
            RequestMetadata updatedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(xPagopaExtchCxId, requestIdx, retrievedRequestPersonal, updatedRequestMetadata);
        });
    }

    @Override
    public Mono<Request> deleteRequest(String xPagopaExtchCxId, String requestIdx) {
        return Mono.zip(requestPersonalService.deleteRequestPersonal(xPagopaExtchCxId, requestIdx), requestMetadataService.deleteRequestMetadata(xPagopaExtchCxId,requestIdx))
                   .map(objects -> {
                       RequestPersonal deletedRequestPersonal = objects.getT1();
                       RequestMetadata deletedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(xPagopaExtchCxId, requestIdx, deletedRequestPersonal, deletedRequestMetadata);
                   });
    }
}
