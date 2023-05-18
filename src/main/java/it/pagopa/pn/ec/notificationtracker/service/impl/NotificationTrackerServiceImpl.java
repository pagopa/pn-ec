package it.pagopa.pn.ec.notificationtracker.service.impl;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
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
import software.amazon.ion.Timestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.SECONDS;


@Service
@Slf4j
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEvents putEvents;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMachinaStati;
    private final SqsService sqsService;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public NotificationTrackerServiceImpl(PutEvents putEvents, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMacchinaStati callMachinaStati, SqsService sqsService,
                                          TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.putEvents = putEvents;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMachinaStati = callMachinaStati;
        this.sqsService = sqsService;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    public Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String processId,
                                                String ntStatoQueueName, String ntStatoErroreQueueName, Acknowledgment acknowledgment) {
        var nextStatus = notificationTrackerQueueDto.getNextStatus();
        var xPagopaExtchCxId = notificationTrackerQueueDto.getXPagopaExtchCxId();

        log.info("<-- Start handleRequestStatusChange --> info: {} request: {}", processId, notificationTrackerQueueDto.getRequestIdx());

        return gestoreRepositoryCall.getRichiesta(notificationTrackerQueueDto.getXPagopaExtchCxId(),
                                                  notificationTrackerQueueDto.getRequestIdx())
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
                                            paperProgressStatusDto.statusDescription(nextStatus)
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
                                                legalMessageSentDetails.setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                                legalMessageSentDetails.setEventDetails(lastEventUpdatedDigital.getEventDetails());
                                                legalMessageSentDetails.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
                                                legalMessageSentDetails.setGeneratedMessage(digitalMessageReference);

                                                singleStatusUpdate.setDigitalLegal(legalMessageSentDetails);

                                            } else {

                                                CourtesyMessageProgressEvent courtesyMessageProgressEvent = new CourtesyMessageProgressEvent();

                                                courtesyMessageProgressEvent.setRequestId(requestDto.getRequestIdx());
                                                courtesyMessageProgressEvent.setStatus(Enum.valueOf(ProgressEventCategory.class, macchinaStatiDecodeResponseDto.getExternalStatus()));
                                                courtesyMessageProgressEvent.setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
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
                                            paperProgressStatusEvent.setProductType(paperRequest.getProductType());
                                            paperProgressStatusEvent.setIun(paperRequest.getIun());
                                            paperProgressStatusEvent.setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                                            paperProgressStatusEvent.setStatusDescription(macchinaStatiDecodeResponseDto.getExternalStatus());
                                            paperProgressStatusEvent.setStatusDateTime(lastEventUpdatedPaper.getStatusDateTime());
                                            paperProgressStatusEvent.setDeliveryFailureCause(lastEventUpdatedPaper.getDeliveryFailureCause());

                                            var attachmentsDetails = new AttachmentDetails();
                                            var attachmentsDetailsList = new ArrayList<AttachmentDetails>();

                                            List<AttachmentsProgressEventDto> attachmentsProgressEventDtolist =
                                                    lastEventUpdatedPaper.getAttachments();

                                            for (AttachmentsProgressEventDto attachmentsProgressEventDto :
                                                    attachmentsProgressEventDtolist) {
                                                attachmentsDetails.setId(attachmentsProgressEventDto.getId());
                                                attachmentsDetails.setDocumentType(attachmentsProgressEventDto.getDocumentType());
                                                attachmentsDetails.setUrl(attachmentsProgressEventDto.getUri());
                                                attachmentsDetails.setDate(attachmentsProgressEventDto.getDate());
                                                attachmentsDetailsList.add(attachmentsDetails);
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

                                            paperProgressStatusEvent.setClientRequestTimeStamp(requestDto.getRequestTimeStamp());

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
                                    });
    }
}
