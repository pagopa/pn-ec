package it.pagopa.pn.ec.notificationtracker.service.impl;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsMaxTimeElapsedException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static it.pagopa.pn.ec.commons.utils.CompareUtils.*;
import static java.time.temporal.ChronoUnit.SECONDS;


@Service
@Slf4j
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

        log.info("<-- Start handleRequestStatusChange --> info: {} request: {}", processId, sRequestId);

        return gestoreRepositoryCall.getRichiesta(xPagopaExtchCxId, sRequestId)

                                     // Check if the incoming event is equals to the last event that was worked on.
                                    .flatMap(requestDto ->
                                    {
                                        List<EventsDto> eventsList = requestDto.getRequestMetadata().getEventsList();
                                        boolean isSameEvent = false;

                                        if (!Objects.isNull(eventsList) && !eventsList.isEmpty()) {

                                            EventsDto lastEvent = eventsList.get(eventsList.size() - 1);
                                            PaperProgressStatusDto paperProgressStatusDto = notificationTrackerQueueDto.getPaperProgressStatusDto();
                                            DigitalProgressStatusDto digitalProgressStatusDto = notificationTrackerQueueDto.getDigitalProgressStatusDto();

                                            log.debug("handleRequestStatusChange - eventsList : {} , paperProgressStatusDto : {}, digitalProgressStatusDto : {}", eventsList, paperProgressStatusDto, digitalProgressStatusDto);

                                            if (lastEvent.getDigProgrStatus() != null) {
                                                log.debug("handleRequestStatusChange - LastEvent digitalProgressStatus : {}", lastEvent.getDigProgrStatus());
                                                isSameEvent = isSameEvent(lastEvent.getDigProgrStatus(), digitalProgressStatusDto, notificationTrackerQueueDto.getNextStatus());
                                            } else {
                                                log.debug("handleRequestStatusChange - LastEvent paperProgressStatus : {}", lastEvent.getPaperProgrStatus());
                                                isSameEvent = isSameEvent(lastEvent.getPaperProgrStatus(), paperProgressStatusDto, notificationTrackerQueueDto.getNextStatus());
                                            }
                                        }

                                        log.debug("handleRequestStatusChange - isSameEvent : {}", isSameEvent);
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
                                            digitalProgressStatusDto.status(nextStatus)
                                                                    .statusCode(logicStatus)
                                                                    .eventTimestamp(digitalProgressStatusDto.getEventTimestamp()
                                                                                                            .truncatedTo(SECONDS));
                                        } else if (paperProgressStatusDto != null) {
                                            paperProgressStatusDto.status(nextStatus)
                                                                  .statusDescription(paperProgressStatusDto.getStatusDescription())
                                                                  .statusCode(macchinaStatiDecodeResponseDto.getLogicStatus())
                                                                  .statusDateTime(paperProgressStatusDto.getStatusDateTime()
                                                                                                        .truncatedTo(SECONDS));
                                        }
                                        return gestoreRepositoryCall.patchRichiestaEvent(notificationTrackerQueueDto.getXPagopaExtchCxId(),
                                                                                         notificationTrackerQueueDto.getRequestIdx(),
                                                                                         new EventsDto().digProgrStatus(
                                                                                                                digitalProgressStatusDto)
                                                                                                        .paperProgrStatus(
                                                                                                                paperProgressStatusDto));
                                    })
                                    .filter(objects -> objects.getT1().getLogicStatus() != null)
                                    .flatMap(objects -> {

                                        var macchinaStatiDecodeResponseDto=objects.getT1();
                                        var requestDto=objects.getT2();
                                        var lastEventIndex = requestDto.getRequestMetadata().getEventsList().size() - 1;

                                        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();

                                        if (notificationTrackerQueueDto.getDigitalProgressStatusDto() != null) {

                                            var lastEventUpdatedDigital = requestDto.getRequestMetadata().getEventsList().get(lastEventIndex).getDigProgrStatus();

                                            var digitalMessageReference = new DigitalMessageReference();
                                            var generatedMessage = lastEventUpdatedDigital.getGeneratedMessage();
                                            digitalMessageReference.setId(generatedMessage.getId());
                                            digitalMessageReference.setSystem(generatedMessage.getSystem());
                                            digitalMessageReference.setLocation(generatedMessage.getLocation());

                                            if (transactionProcessConfigurationProperties.pec().equals(processId)) {

                                                LegalMessageSentDetails legalMessageSentDetails = new LegalMessageSentDetails();

                                                legalMessageSentDetails.setRequestId(requestDto.getRequestIdx());
                                                legalMessageSentDetails.setStatus(Enum.valueOf(ProgressEventCategory.class, macchinaStatiDecodeResponseDto.getExternalStatus()));
                                                legalMessageSentDetails.setEventCode(LegalMessageSentDetails.EventCodeEnum.fromValue(macchinaStatiDecodeResponseDto.getLogicStatus()));
                                                legalMessageSentDetails.setEventDetails(lastEventUpdatedDigital.getEventDetails());
                                                legalMessageSentDetails.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
                                                legalMessageSentDetails.setGeneratedMessage(digitalMessageReference);

                                                singleStatusUpdate.setDigitalLegal(legalMessageSentDetails);

                                            } else {

                                                CourtesyMessageProgressEvent courtesyMessageProgressEvent = new CourtesyMessageProgressEvent();

                                                courtesyMessageProgressEvent.setRequestId(requestDto.getRequestIdx());
                                                courtesyMessageProgressEvent.setStatus(Enum.valueOf(ProgressEventCategory.class, macchinaStatiDecodeResponseDto.getExternalStatus()));
                                                courtesyMessageProgressEvent.setEventCode(CourtesyMessageProgressEvent.EventCodeEnum.fromValue(macchinaStatiDecodeResponseDto.getLogicStatus()));
                                                courtesyMessageProgressEvent.setEventDetails(lastEventUpdatedDigital.getEventDetails());
                                                courtesyMessageProgressEvent.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
                                                courtesyMessageProgressEvent.setGeneratedMessage(digitalMessageReference);

                                                singleStatusUpdate.setDigitalCourtesy(courtesyMessageProgressEvent);

                                            }
                                        } else {
                                            PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();

                                            var lastEventUpdatedPaper = requestDto.getRequestMetadata().getEventsList().get(lastEventIndex).getPaperProgrStatus();

                                            paperProgressStatusEvent.setRequestId(requestDto.getRequestIdx());
                                            paperProgressStatusEvent.setRegisteredLetterCode(lastEventUpdatedPaper.getRegisteredLetterCode());

                                            var paperRequest = requestDto.getRequestMetadata().getPaperRequestMetadata();
                                            paperProgressStatusEvent.setProductType(lastEventUpdatedPaper.getProductType());
                                            paperProgressStatusEvent.setIun(lastEventUpdatedPaper.getIun());
                                            paperProgressStatusEvent.setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                            paperProgressStatusEvent.setStatusDescription(lastEventUpdatedPaper.getStatusDescription());
                                            paperProgressStatusEvent.setStatusDateTime(lastEventUpdatedPaper.getStatusDateTime());
                                            paperProgressStatusEvent.setDeliveryFailureCause(lastEventUpdatedPaper.getDeliveryFailureCause());

                                            var attachmentsDetailsList = new ArrayList<AttachmentDetails>();

                                            List<AttachmentsProgressEventDto> attachmentsProgressEventDtolist= new ArrayList<>();

                                            if(!Objects.isNull(lastEventUpdatedPaper.getAttachments())) {
                                                attachmentsProgressEventDtolist = lastEventUpdatedPaper.getAttachments();
                                                for (AttachmentsProgressEventDto attachmentsProgressEventDto :
                                                        attachmentsProgressEventDtolist) {
                                                    var attachmentsDetails = new AttachmentDetails();
                                                    attachmentsDetails.setSha256(attachmentsProgressEventDto.getSha256());
                                                    attachmentsDetails.setId(attachmentsProgressEventDto.getId());
                                                    attachmentsDetails.setDocumentType(attachmentsProgressEventDto.getDocumentType());
                                                    attachmentsDetails.setUri(attachmentsProgressEventDto.getUri());
                                                    attachmentsDetails.setDate(attachmentsProgressEventDto.getDate());
                                                    attachmentsDetailsList.add(attachmentsDetails);
                                                }
                                            }
                                            paperProgressStatusEvent.setAttachments(attachmentsDetailsList);
                                            var lastDiscoveredAddress = lastEventUpdatedPaper.getDiscoveredAddress();
                                            if (!Objects.isNull(lastDiscoveredAddress)) {
                                                var discoveredAddress = new DiscoveredAddress();
                                                discoveredAddress.setName(lastDiscoveredAddress.getName());
                                                discoveredAddress.setNameRow2(lastDiscoveredAddress.getNameRow2());
                                                discoveredAddress.setAddress(lastDiscoveredAddress.getAddress());
                                                discoveredAddress.setAddressRow2(lastDiscoveredAddress.getAddressRow2());
                                                discoveredAddress.setCap(lastDiscoveredAddress.getCap());
                                                discoveredAddress.setCity(lastDiscoveredAddress.getCity());
                                                discoveredAddress.setCity2(lastDiscoveredAddress.getCity2());
                                                discoveredAddress.setPr(lastDiscoveredAddress.getPr());
                                                discoveredAddress.setCountry(lastDiscoveredAddress.getCountry());
                                                paperProgressStatusEvent.setDiscoveredAddress(discoveredAddress);
                                            }

                                            paperProgressStatusEvent.setClientRequestTimeStamp(lastEventUpdatedPaper.getClientRequestTimeStamp());

                                            singleStatusUpdate.setAnalogMail(paperProgressStatusEvent);
                                        }
                                        singleStatusUpdate.setClientId(requestDto.getxPagopaExtchCxId());
                                        singleStatusUpdate.setEventTimestamp(OffsetDateTime.now());
                                        return putEvents.putEventExternal(singleStatusUpdate, processId);
                                    })
                                    .doOnSuccess(result -> acknowledgment.acknowledge())
                                    .then()
                                    .doOnError(InvalidNextStatusException.class, throwable -> {
                                        log.debug("Invalid Next Status Exception: {}", throwable.getMessage());
                                        acknowledgment.acknowledge();
                                    })
                                    .onErrorResume(InvalidNextStatusException.class, e -> {
                                        var retry = notificationTrackerQueueDto.getRetry();
                                        notificationTrackerQueueDto.setRetry(retry + 1);
                                        if (retry < 5) {
                                            return sqsService.send(ntStatoQueueName, notificationTrackerQueueDto).then();
                                        } else {
                                            return sqsService.send(ntStatoErroreQueueName, notificationTrackerQueueDto).then();
                                        }
                                    })
                                    .doOnError(throwable -> {
                                        log.warn("handleRequestStatusChange on request {}: {} - {}", sRequestId, throwable, throwable.getMessage());
                                    });
    }

    @Override
    public Mono<Void> handleMessageFromErrorQueue(NotificationTrackerQueueDto notificationTrackerQueueDto,
                                                  String ntStatoQueueName, Acknowledgment acknowledgment) {

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
                .doOnError(throwable -> log.warn("Exception in handleMessageFromErrorQueue() on request {}: {} - {}", notificationTrackerQueueDto.getRequestIdx(), throwable, throwable.getMessage()))
                .then();
    }


}
