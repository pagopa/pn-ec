package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.*;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Patch;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;
import it.pagopa.pn.ec.repositorymanager.service.RequestMetadataService;
import it.pagopa.pn.ec.repositorymanager.service.RequestPersonalService;
import it.pagopa.pn.ec.repositorymanager.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.StreamUtils.getStreamOfNullableList;
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

    public static String concatRequestId(String clientId, String requestId) {
        return (String.format("%s%s%s", clientId, SEPARATORE, requestId));
    }

    private String createRequestHash(List<String> sourceStrings) {
        return DigestUtils.sha256Hex(sourceStrings.stream()
                                                  .filter(Objects::nonNull)
                                                  .filter(Predicate.not(String::isEmpty))
                                                  .map(String::toLowerCase)
                                                  .map(StringUtils::normalizeSpace)
                                                  .map(StringUtils::deleteWhitespace)
                                                  .collect(Collectors.joining(SEPARATORE)));
    }

    private List<String> retrieveFieldsToHashFromDigitalRequestPersonal(DigitalRequestPersonal digitalRequestPersonal) {
        return Stream.concat(Stream.of(digitalRequestPersonal.getReceiverDigitalAddress(),
                                       digitalRequestPersonal.getMessageText(),
                                       digitalRequestPersonal.getSenderDigitalAddress(),
                                       digitalRequestPersonal.getSubjectText()),
                             getStreamOfNullableList(digitalRequestPersonal.getAttachmentsUrls())).toList();
    }

    private List<String> retrieveFieldsToHashFromPaperRequestPersonal(PaperRequestPersonal paperRequestPersonal) {
        return Stream.concat(Stream.of(paperRequestPersonal.getReceiverName(),
                                       paperRequestPersonal.getReceiverNameRow2(),
                                       paperRequestPersonal.getReceiverAddress(),
                                       paperRequestPersonal.getReceiverNameRow2(),
                                       paperRequestPersonal.getReceiverCap(),
                                       paperRequestPersonal.getReceiverCity(),
                                       paperRequestPersonal.getReceiverCity2(),
                                       paperRequestPersonal.getReceiverPr(),
                                       paperRequestPersonal.getReceiverCountry(),
                                       paperRequestPersonal.getReceiverFiscalCode(),
                                       paperRequestPersonal.getSenderName(),
                                       paperRequestPersonal.getSenderAddress(),
                                       paperRequestPersonal.getSenderCity(),
                                       paperRequestPersonal.getSenderPr(),
                                       paperRequestPersonal.getSenderDigitalAddress(),
                                       paperRequestPersonal.getArName(),
                                       paperRequestPersonal.getArAddress(),
                                       paperRequestPersonal.getArCap(),
                                       paperRequestPersonal.getArCity()),
                             getStreamOfNullableList(paperRequestPersonal.getAttachments()).map(PaperEngageRequestAttachments::getUri))
                     .toList();
    }

    @Override
    public Mono<Request> getRequest(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL, GET_REQUEST_OP, concatRequest);
        return Mono.zip(requestPersonalService.getRequestPersonal(concatRequest), requestMetadataService.getRequestMetadata(concatRequest))
                   .map(objects -> {
                       RequestPersonal retrievedRequestPersonal = objects.getT1();
                       RequestMetadata retrievedRequestMetadata = objects.getT2();
                       return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                   }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_REQUEST_OP, result));
    }

    @Override
    public Mono<Request> insertRequest(Request request) {
        log.debug(INVOKING_OPERATION_LABEL, INSERT_REQUEST_OP, request.getRequestId());
        return Mono.fromCallable(() -> {
                       var concatRequestId = concatRequestId(request.getXPagopaExtchCxId(), request.getRequestId());
                       var clientId = request.getXPagopaExtchCxId();

                       var requestTimestamp = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

                       var requestPersonal = request.getRequestPersonal();
                       requestPersonal.setRequestId(concatRequestId);
                       requestPersonal.setXPagopaExtchCxId(clientId);
                       requestPersonal.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
                       requestPersonal.setRequestTimestamp(requestTimestamp);

                       var requestMetadata = request.getRequestMetadata();
                       requestMetadata.setRequestId(concatRequestId);
                       requestMetadata.setXPagopaExtchCxId(clientId);
                       requestMetadata.setClientRequestTimeStamp(request.getClientRequestTimeStamp());
                       requestMetadata.setRequestTimestamp(requestTimestamp);

                       return request;
                   })
                   .handle((requestPersonalAndMetadata, sink) -> {
                       var requestPersonal = requestPersonalAndMetadata.getRequestPersonal();
                       var requestMetadata = requestPersonalAndMetadata.getRequestMetadata();

                       if ((requestPersonal.getDigitalRequestPersonal() != null && requestMetadata.getPaperRequestMetadata() != null) ||
                           (requestPersonal.getPaperRequestPersonal() != null && requestMetadata.getDigitalRequestMetadata() != null)) {
                           sink.error(new RepositoryManagerException.RequestMalformedException("Incompatibilità dati sensibili con metadata"));
                       } else {
                           sink.next(requestPersonalAndMetadata);
                       }
                   })
                   .flatMap(object -> {
                       var requestPersonal = request.getRequestPersonal();
                       var digitalRequestPersonal = requestPersonal.getDigitalRequestPersonal();
                       var paperRequestPersonal = requestPersonal.getPaperRequestPersonal();
                       List<String> fieldsToHash;
                       var requestMetadata = request.getRequestMetadata();
                       if (digitalRequestPersonal != null) {
                           fieldsToHash = retrieveFieldsToHashFromDigitalRequestPersonal(digitalRequestPersonal);
                       } else {
                           fieldsToHash = retrieveFieldsToHashFromPaperRequestPersonal(paperRequestPersonal);
                       }
                       requestMetadata.setRequestHash(createRequestHash(fieldsToHash));
                       return requestMetadataService.insertRequestMetadata(requestMetadata);
                   })
                   .zipWhen(requestMetadata -> requestPersonalService.insertRequestPersonal(request.getRequestPersonal())
                                                                     .onErrorResume(throwable -> {
                                                                         var requestId = request.getRequestId();
                                                                         var concatRequestId =
                                                                                 concatRequestId(request.getXPagopaExtchCxId(), requestId);
                                                                         return requestMetadataService.deleteRequestMetadata(concatRequestId)
                                                                                                      .then(Mono.error(throwable));
                                                                     }))
                   .map(objects -> {
                       var insertedRequestMetadata = objects.getT1();
                       var insertedRequestPersonal = objects.getT2();
                       return createRequestFromPersonalAndMetadata(insertedRequestPersonal, insertedRequestMetadata);
                   }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, INSERT_REQUEST_OP, result));
    }

    @Override
    public Mono<Request> patchRequest(String clientId, String requestIdx, Patch patch) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL, PATCH_REQUEST_OP, concatRequest);
        return Mono.zip(requestPersonalService.getRequestPersonal(concatRequest),
                        requestMetadataService.patchRequestMetadata(concatRequest, patch)).map(objects -> {
            RequestPersonal retrievedRequestPersonal = objects.getT1();
            RequestMetadata updatedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, updatedRequestMetadata);
        }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PATCH_REQUEST_OP, result));
    }

    @Override
    public Mono<Request> deleteRequest(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL, DELETE_REQUEST_OP, concatRequest);
        return Mono.zip(requestPersonalService.deleteRequestPersonal(concatRequest),
                        requestMetadataService.deleteRequestMetadata(concatRequest)).map(objects -> {
            RequestPersonal deletedRequestPersonal = objects.getT1();
            RequestMetadata deletedRequestMetadata = objects.getT2();
            return createRequestFromPersonalAndMetadata(deletedRequestPersonal, deletedRequestMetadata);
        }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, DELETE_REQUEST_OP, result));
    }

    @Override
    public Mono<Request> getRequestByMessageId(String messageId) {
        log.debug(INVOKING_OPERATION_LABEL, GET_REQUEST_BY_MESSAGE_ID_OP, messageId);
        return requestMetadataService.getRequestMetadataByMessageId(messageId)
                                     .zipWhen(requestMetadata -> requestPersonalService.getRequestPersonal(requestMetadata.getRequestId()))
                                     .map(objects -> {
                                         RequestMetadata retrievedRequestMetadata = objects.getT1();
                                         RequestPersonal retrievedRequestPersonal = objects.getT2();
                                         return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                                     }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_REQUEST_BY_MESSAGE_ID_OP, result));
    }

    @Override
    public Mono<Request> setMessageIdInRequestMetadata(String clientId, String requestIdx) {
        var concatRequest = concatRequestId(clientId, requestIdx);
        log.debug(INVOKING_OPERATION_LABEL, SET_MESSAGE_ID_IN_REQUEST_METADATA_OP, concatRequest);
        return requestMetadataService.setMessageIdInRequestMetadata(concatRequest)
                                     .zipWhen(requestMetadata -> requestPersonalService.getRequestPersonal(requestMetadata.getRequestId()))
                                     .map(objects -> {
                                         RequestMetadata retrievedRequestMetadata = objects.getT1();
                                         RequestPersonal retrievedRequestPersonal = objects.getT2();
                                         return createRequestFromPersonalAndMetadata(retrievedRequestPersonal, retrievedRequestMetadata);
                                     }).doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, SET_MESSAGE_ID_IN_REQUEST_METADATA_OP, result));
    }

}
