package it.pagopa.pn.ec.notificationtracker.service.impl;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsMaxTimeElapsedException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static it.pagopa.pn.ec.commons.utils.CompareUtils.*;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static java.time.temporal.ChronoUnit.SECONDS;


@Service
@CustomLog
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEvents putEvents;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMachinaStati;
    private final SqsService sqsService;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    private final NotificationTrackerSqsName notificationTrackerSqsName;

    public NotificationTrackerServiceImpl(PutEvents putEvents, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMacchinaStati callMachinaStati, SqsService sqsService,
                                          TransactionProcessConfigurationProperties transactionProcessConfigurationProperties, NotificationTrackerSqsName notificationTrackerSqsName) {
        this.putEvents = putEvents;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMachinaStati = callMachinaStati;
        this.sqsService = sqsService;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
    }

    @Override
    public Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String processId,
                                                String ntStatoQueueName, String ntStatoErroreQueueName, Acknowledgment acknowledgment) {
        var nextStatus = notificationTrackerQueueDto.getNextStatus();
        var xPagopaExtchCxId = notificationTrackerQueueDto.getXPagopaExtchCxId();
        String sRequestId = notificationTrackerQueueDto.getRequestIdx();
        var concatRequestId=concatRequestId(xPagopaExtchCxId, sRequestId);

        log.info(INVOKING_OPERATION_LABEL_WITH_ARGS,NT_HANDLE_REQUEST_STATUS_CHANGE, concatRequestId);

        return gestoreRepositoryCall.getRichiesta(xPagopaExtchCxId, sRequestId)

                                     // Check if the incoming event is equals to the last event that was worked on.
                                    .flatMap(requestDto ->
                                    {
                                        List<EventsDto> eventsList = requestDto.getRequestMetadata().getEventsList();
                                        boolean isSameEvent = false;

                                        if (!Objects.isNull(eventsList) && !eventsList.isEmpty()) {

                                            DigitalProgressStatusDto digitalProgressStatusDto = notificationTrackerQueueDto.getDigitalProgressStatusDto();

                                            if (digitalProgressStatusDto != null) {
                                                isSameEvent = isSameEvent(eventsList, digitalProgressStatusDto, notificationTrackerQueueDto.getNextStatus());
                                            }
                                        }

                                        log.debug(NT_HANDLE_REQUEST_STATUS_CHANGE + "'{}' - isSameEvent : {}", concatRequestId, isSameEvent);
                                        return isSameEvent ? Mono.empty() : Mono.just(requestDto);
                                    })
//                                  Set status request to start status if is null
                                    .map(requestDto -> {
                                        if (requestDto.getStatusRequest() == null) {
                                            requestDto.setStatusRequest(transactionProcessConfigurationProperties.startStatus());
                                        }
                                        return requestDto;
                                    })
                                    .zipWhen(requestDto -> callMachinaStati.statusValidation(xPagopaExtchCxId,
                                                                                             processId,
                                                                                             requestDto.getStatusRequest(),
                                                                                             notificationTrackerQueueDto.getNextStatus()))
                                    .flatMap(objects -> callMachinaStati.statusDecode(xPagopaExtchCxId,
                                                                                      processId,
                                                                                      notificationTrackerQueueDto.getNextStatus()))
                                    .zipWhen(macchinaStatiDecodeResponseDto -> {
                                        var logicStatus = macchinaStatiDecodeResponseDto.getLogicStatus();
                                        var paperProgressStatusDto = notificationTrackerQueueDto.getPaperProgressStatusDto();
                                        var digitalProgressStatusDto = notificationTrackerQueueDto.getDigitalProgressStatusDto();

                                        if (digitalProgressStatusDto != null) {
                                            OffsetDateTime eventTimestamp = digitalProgressStatusDto.getEventTimestamp();
                                            // Se lo stato è relativo a SERCQ, non bisogna troncare il timestamp.
                                            if (logicStatus != null && !logicStatus.startsWith("Q")) {
                                                eventTimestamp = eventTimestamp.truncatedTo(SECONDS);
                                            }
                                            digitalProgressStatusDto.status(nextStatus)
                                                                    .statusCode(logicStatus)
                                                                    .eventTimestamp(eventTimestamp);
                                        } else if (paperProgressStatusDto != null) {
                                            paperProgressStatusDto.status(nextStatus)
                                                                  .statusDescription(paperProgressStatusDto.getStatusDescription())
                                                                  .statusCode(macchinaStatiDecodeResponseDto.getLogicStatus())
                                                                  .statusDateTime(paperProgressStatusDto.getStatusDateTime()
                                                                                                        .truncatedTo(SECONDS));
                                        }

                                        return gestoreRepositoryCall.patchRichiestaEvent(notificationTrackerQueueDto.getXPagopaExtchCxId(),
                                                                                         notificationTrackerQueueDto.getRequestIdx(),
                                                                                         new EventsDto().digProgrStatus(digitalProgressStatusDto)
                                                                                                        .paperProgrStatus(paperProgressStatusDto));
                                    })
                                    .filter(objects -> objects.getT1().getLogicStatus() != null)
                                    .flatMap(objects -> {

                                        var macchinaStatiDecodeResponseDto=objects.getT1();
                                        var requestDto=objects.getT2();
                                        var lastEventIndex = requestDto.getRequestMetadata().getEventsList().size() - 1;

                                        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();

                                        if (notificationTrackerQueueDto.getDigitalProgressStatusDto() != null) {

                                            var lastEventUpdatedDigital = requestDto.getRequestMetadata().getEventsList().get(lastEventIndex).getDigProgrStatus();

                                            DigitalMessageReference digitalMessageReference = null;
                                            var generatedMessage = lastEventUpdatedDigital.getGeneratedMessage();

                                            if (generatedMessage != null) {
                                                digitalMessageReference = new DigitalMessageReference();
                                                digitalMessageReference.setId(generatedMessage.getId());
                                                digitalMessageReference.setSystem(generatedMessage.getSystem());
                                                digitalMessageReference.setLocation(generatedMessage.getLocation());
                                            }

                                            if (transactionProcessConfigurationProperties.pec().equals(processId) || transactionProcessConfigurationProperties.sercq().equals(processId)) {

                                                LegalMessageSentDetails legalMessageSentDetails = createLegalMessageSentDetails(requestDto, macchinaStatiDecodeResponseDto, lastEventUpdatedDigital, digitalMessageReference);

                                                singleStatusUpdate.setDigitalLegal(legalMessageSentDetails);

                                            } else {

                                                CourtesyMessageProgressEvent courtesyMessageProgressEvent = createCourtesyMessageProgressEvent(requestDto, macchinaStatiDecodeResponseDto, lastEventUpdatedDigital, digitalMessageReference);

                                                singleStatusUpdate.setDigitalCourtesy(courtesyMessageProgressEvent);

                                            }
                                        } else {

                                            var lastEventUpdatedPaper = requestDto.getRequestMetadata().getEventsList().get(lastEventIndex).getPaperProgrStatus();

                                            PaperProgressStatusEvent paperProgressStatusEvent = createPaperProgressStatusEvent(requestDto, macchinaStatiDecodeResponseDto, lastEventUpdatedPaper);

                                            var attachmentsDetailsList = new ArrayList<AttachmentDetails>();

                                            List<AttachmentsProgressEventDto> attachmentsProgressEventDtolist;

                                            if(!Objects.isNull(lastEventUpdatedPaper.getAttachments())) {
                                                attachmentsProgressEventDtolist = lastEventUpdatedPaper.getAttachments();
                                                for (AttachmentsProgressEventDto attachmentsProgressEventDto :
                                                        attachmentsProgressEventDtolist) {
                                                    var attachmentsDetails = createAttachmentDetails(attachmentsProgressEventDto);
                                                    attachmentsDetailsList.add(attachmentsDetails);
                                                }
                                            }
                                            paperProgressStatusEvent.setAttachments(attachmentsDetailsList);
                                            var lastDiscoveredAddress = lastEventUpdatedPaper.getDiscoveredAddress();
                                            if (!Objects.isNull(lastDiscoveredAddress)) {
                                                var discoveredAddress = createDiscoverdAddress(lastDiscoveredAddress);
                                                paperProgressStatusEvent.setDiscoveredAddress(discoveredAddress);
                                            }

                                            paperProgressStatusEvent.setClientRequestTimeStamp(lastEventUpdatedPaper.getClientRequestTimeStamp());

                                            singleStatusUpdate.setAnalogMail(paperProgressStatusEvent);
                                        }
                                        singleStatusUpdate.setClientId(requestDto.getxPagopaExtchCxId());
                                        singleStatusUpdate.setEventTimestamp(OffsetDateTime.now());
                                        return putEvents.putEventExternal(singleStatusUpdate, processId);
                                    })
                                    .then()
                                    .doOnError(InvalidNextStatusException.class, throwable -> log.debug(EXCEPTION_IN_PROCESS_FOR, NT_HANDLE_REQUEST_STATUS_CHANGE, concatRequestId, throwable, throwable.getMessage()))
                                    .onErrorResume(InvalidNextStatusException.class, e -> {
                                        var retry = notificationTrackerQueueDto.getRetry();
                                        notificationTrackerQueueDto.setRetry(retry + 1);
                                        if (retry < 5) {
                                            return sqsService.send(ntStatoQueueName, notificationTrackerQueueDto).then();
                                        } else {
                                            return sqsService.send(ntStatoErroreQueueName, notificationTrackerQueueDto).then();
                                        }
                                    })
                                    .doOnSuccess(result -> {
                                        acknowledgment.acknowledge();
                                        log.info(SUCCESSFUL_OPERATION_ON_LABEL, concatRequestId, NT_HANDLE_REQUEST_STATUS_CHANGE, result);
                                    })
                                    .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, NT_HANDLE_REQUEST_STATUS_CHANGE, concatRequestId, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<Void> handleMessageFromErrorQueue(NotificationTrackerQueueDto notificationTrackerQueueDto,
                                                  String ntStatoQueueName, Acknowledgment acknowledgment) {
        var concatRequestId = concatRequestId(notificationTrackerQueueDto.getXPagopaExtchCxId(), notificationTrackerQueueDto.getRequestIdx());
        return Mono.just(notificationTrackerQueueDto)
                .flatMap(payload -> {
                    var digitalProgressStatusDto = payload.getDigitalProgressStatusDto();
                    var paperProgressStatusDto = payload.getPaperProgressStatusDto();

                    long elapsedTime = 0;
                    var now = OffsetDateTime.now();

                    if (digitalProgressStatusDto != null) {
                        elapsedTime = SECONDS.between(digitalProgressStatusDto.getEventTimestamp(), now);

                    } else elapsedTime = SECONDS.between(paperProgressStatusDto.getStatusDateTime(), now);

                    return elapsedTime > notificationTrackerSqsName.elapsedTimeSeconds() ? Mono.error(new SqsMaxTimeElapsedException()) : Mono.just(payload);
                })
                .doOnNext(payload -> payload.setRetry(0))
                .flatMap(payload -> sqsService.send(ntStatoQueueName, payload))
                .doOnSuccess(result -> acknowledgment.acknowledge())
                .doOnError(throwable -> log.warn(EXCEPTION_IN_PROCESS_FOR, NT_HANDLE_MESSAGE_FROM_ERROR_QUEUE, concatRequestId, throwable, throwable.getMessage()))
                .then();
    }


    private LegalMessageSentDetails createLegalMessageSentDetails(RequestDto requestDto, MacchinaStatiDecodeResponseDto macchinaStatiDecodeResponseDto, DigitalProgressStatusDto lastEventUpdatedDigital, DigitalMessageReference digitalMessageReference){
        LegalMessageSentDetails legalMessageSentDetails = new LegalMessageSentDetails();
        legalMessageSentDetails.setRequestId(requestDto.getRequestIdx());
        legalMessageSentDetails.setStatus(Enum.valueOf(ProgressEventCategory.class, macchinaStatiDecodeResponseDto.getExternalStatus()));
        legalMessageSentDetails.setEventCode(LegalMessageSentDetails.EventCodeEnum.fromValue(macchinaStatiDecodeResponseDto.getLogicStatus()));
        legalMessageSentDetails.setEventDetails(lastEventUpdatedDigital.getEventDetails());
        legalMessageSentDetails.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
        legalMessageSentDetails.setGeneratedMessage(digitalMessageReference);
        return legalMessageSentDetails;
    }

    private CourtesyMessageProgressEvent createCourtesyMessageProgressEvent(RequestDto requestDto, MacchinaStatiDecodeResponseDto macchinaStatiDecodeResponseDto, DigitalProgressStatusDto lastEventUpdatedDigital, DigitalMessageReference digitalMessageReference){
        CourtesyMessageProgressEvent courtesyMessageProgressEvent = new CourtesyMessageProgressEvent();
        courtesyMessageProgressEvent.setRequestId(requestDto.getRequestIdx());
        courtesyMessageProgressEvent.setStatus(Enum.valueOf(ProgressEventCategory.class, macchinaStatiDecodeResponseDto.getExternalStatus()));
        courtesyMessageProgressEvent.setEventCode(CourtesyMessageProgressEvent.EventCodeEnum.fromValue(macchinaStatiDecodeResponseDto.getLogicStatus()));
        courtesyMessageProgressEvent.setEventDetails(lastEventUpdatedDigital.getEventDetails());
        courtesyMessageProgressEvent.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
        courtesyMessageProgressEvent.setGeneratedMessage(digitalMessageReference);
        return courtesyMessageProgressEvent;

    }

    private PaperProgressStatusEvent createPaperProgressStatusEvent(RequestDto requestDto, MacchinaStatiDecodeResponseDto macchinaStatiDecodeResponseDto, PaperProgressStatusDto lastEventUpdatedPaper){
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setRequestId(requestDto.getRequestIdx());
        paperProgressStatusEvent.setRegisteredLetterCode(lastEventUpdatedPaper.getRegisteredLetterCode());
        paperProgressStatusEvent.setProductType(lastEventUpdatedPaper.getProductType());
        paperProgressStatusEvent.setIun(lastEventUpdatedPaper.getIun());
        paperProgressStatusEvent.setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
        paperProgressStatusEvent.setStatusDescription(lastEventUpdatedPaper.getStatusDescription());
        paperProgressStatusEvent.setStatusDateTime(lastEventUpdatedPaper.getStatusDateTime());
        paperProgressStatusEvent.setDeliveryFailureCause(lastEventUpdatedPaper.getDeliveryFailureCause());
        return paperProgressStatusEvent;
    }

    private AttachmentDetails createAttachmentDetails(AttachmentsProgressEventDto attachmentsProgressEventDto){
        AttachmentDetails attachmentsDetails = new AttachmentDetails();
        attachmentsDetails.setSha256(attachmentsProgressEventDto.getSha256());
        attachmentsDetails.setId(attachmentsProgressEventDto.getId());
        attachmentsDetails.setDocumentType(attachmentsProgressEventDto.getDocumentType());
        attachmentsDetails.setUri(attachmentsProgressEventDto.getUri());
        attachmentsDetails.setDate(attachmentsProgressEventDto.getDate());
        return  attachmentsDetails;
    }

    private DiscoveredAddress createDiscoverdAddress(DiscoveredAddressDto lastDiscoveredAddress){
        DiscoveredAddress discoveredAddress = new DiscoveredAddress();
        discoveredAddress.setName(lastDiscoveredAddress.getName());
        discoveredAddress.setNameRow2(lastDiscoveredAddress.getNameRow2());
        discoveredAddress.setAddress(lastDiscoveredAddress.getAddress());
        discoveredAddress.setAddressRow2(lastDiscoveredAddress.getAddressRow2());
        discoveredAddress.setCap(lastDiscoveredAddress.getCap());
        discoveredAddress.setCity(lastDiscoveredAddress.getCity());
        discoveredAddress.setCity2(lastDiscoveredAddress.getCity2());
        discoveredAddress.setPr(lastDiscoveredAddress.getPr());
        discoveredAddress.setCountry(lastDiscoveredAddress.getCountry());
        return  discoveredAddress;
    }
}
