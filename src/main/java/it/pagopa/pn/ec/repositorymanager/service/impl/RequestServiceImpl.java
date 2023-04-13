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
    private static final String SEPARATORE = "~";

    public RequestServiceImpl(RequestPersonalService requestPersonalService, RequestMetadataService requestMetadataService) {
        this.requestPersonalService = requestPersonalService;
        this.requestMetadataService = requestMetadataService;
    }

    private String concatRequestId(String clientId, String requestId) {
        return (clientId + SEPARATORE + requestId);
    }

    @Override
    public Mono<Request> getRequest(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        return Mono.zip(requestPersonalService.getRequestPersonal(concatRequest), requestMetadataService.getRequestMetadata(concatRequest))
                   .map(objects -> {
                       RequestPersonal retrievedRequestPersonal = objects.getT1();
                       RequestMetadata retrievedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                   });
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        return Mono.fromCallable(() -> {
                       var requestId = concatRequestId(request.getXPagopaExtchCxId(), request.getRequestId());
                       var clientId = request.getXPagopaExtchCxId();

                       var requestTimestamp = OffsetDateTime.now();

                       var requestPersonal = request.getRequestPersonal();
                       requestPersonal.setRequestId(requestId);
                       requestPersonal.setXPagopaExtchCxId(clientId);
                       requestPersonal.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
                       requestPersonal.setRequestTimestamp(requestTimestamp);

                       var requestMetadata = request.getRequestMetadata();
                       requestMetadata.setRequestId(requestId);
                       requestMetadata.setXPagopaExtchCxId(clientId);
                       requestMetadata.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
                       requestMetadata.setRequestTimestamp(requestTimestamp);

                       return request;
                   })
                   .handle((objects, sink) -> {
                       var requestPersonal = objects.getRequestPersonal();
                       var requestMetadata = objects.getRequestMetadata();

                       if ((requestPersonal.getDigitalRequestPersonal() != null && requestMetadata.getPaperRequestMetadata() != null) ||
                           (requestPersonal.getPaperRequestPersonal() != null && requestMetadata.getDigitalRequestMetadata() != null)) {
                           sink.error(new RepositoryManagerException.RequestMalformedException(
                                   "Incompatibilità dati sensibili con " + "metadata"));
                       } else {
                           sink.next(objects);
                       }
                   })
                   .flatMap(objects -> requestPersonalService.insertRequestPersonal(request.getRequestPersonal()))
                   .zipWhen(requestPersonal -> requestMetadataService.insertRequestMetadata(request.getRequestMetadata())
                                                                     .onErrorResume(throwable -> {
                                                                         var requestId = request.getRequestId();
                                                                         var concatRequestId =
                                                                                 concatRequestId(request.getXPagopaExtchCxId(), requestId);
                                                                         return requestPersonalService.deleteRequestPersonal(concatRequestId)
                                                                                                      .then(Mono.error(throwable));
                                                                     }))
                   .map(objects -> {
                       RequestPersonal insertedRequestPersonal = objects.getT1();
                       RequestMetadata insertedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(insertedRequestPersonal, insertedRequestMetadata);
                   });
    }

    @Override
    public Mono<Request> patchRequest(String clientId, String requestIdx, Patch patch) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        return Mono.zip(requestPersonalService.getRequestPersonal(concatRequest),
                        requestMetadataService.patchRequestMetadata(concatRequest, patch)).map(objects -> {
            RequestPersonal retrievedRequestPersonal = objects.getT1();
            RequestMetadata updatedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, updatedRequestMetadata);
        });
    }

    @Override
    public Mono<Request> deleteRequest(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        return Mono.zip(requestPersonalService.deleteRequestPersonal(concatRequest),
                        requestMetadataService.deleteRequestMetadata(concatRequest)).map(objects -> {
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
    public Mono<Request> setMessageIdInRequestMetadata(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        return requestMetadataService.setMessageIdInRequestMetadata(concatRequest)
                                     .zipWhen(requestMetadata -> requestPersonalService.getRequestPersonal(requestMetadata.getRequestId()))
                                     .map(objects -> {
                                         RequestMetadata retrievedRequestMetadata = objects.getT1();
                                         RequestPersonal retrievedRequestPersonal = objects.getT2();
                                         return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                                     });
    }

}
