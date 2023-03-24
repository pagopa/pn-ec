package it.pagopa.pn.ec.notificationtracker.service.impl;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.notificationtracker.service.NotificationTrackerService;
import it.pagopa.pn.ec.notificationtracker.service.PutEvents;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;


@Service
public class NotificationTrackerServiceImpl implements NotificationTrackerService {

    private final PutEvents putEvents;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMachinaStati;
    private final SqsService sqsService;

    public NotificationTrackerServiceImpl(PutEvents putEvents, GestoreRepositoryCall gestoreRepositoryCall,
                                          CallMacchinaStati callMachinaStati, SqsService sqsService) {
        this.putEvents = putEvents;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMachinaStati = callMachinaStati;
        this.sqsService = sqsService;
    }

    @Override
    public Mono<Void> handleRequestStatusChange(NotificationTrackerQueueDto notificationTrackerQueueDto, String ntStatoQueueName, String ntStatoDlQueueName, Acknowledgment acknowledgment) {
        String nextStatus = notificationTrackerQueueDto.getNextStatus();
        return callMachinaStati.statusValidation(notificationTrackerQueueDto)
                .flatMap(unused -> {
                    notificationTrackerQueueDto.setCurrentStatus(nextStatus);
                    return callMachinaStati.statusDecode(notificationTrackerQueueDto);
                })
                .zipWhen(macchinaStatiDecodeResponseDto -> {
                    var logicStatus = macchinaStatiDecodeResponseDto.getLogicStatus();
                    var paperProgressStatusDto = notificationTrackerQueueDto.getPaperProgressStatusDto();
                    var digitalProgressStatusDto = notificationTrackerQueueDto.getDigitalProgressStatusDto();

                    if (digitalProgressStatusDto != null) {
                        digitalProgressStatusDto.status(nextStatus)
                                .statusCode(logicStatus)
                                .eventTimestamp(digitalProgressStatusDto.getEventTimestamp().truncatedTo(SECONDS));
                    } else if (paperProgressStatusDto != null) {
                        paperProgressStatusDto.statusDescription(nextStatus)
                                .statusCode(macchinaStatiDecodeResponseDto.getLogicStatus())
                                .statusDateTime(paperProgressStatusDto.getStatusDateTime().truncatedTo(SECONDS));
                    }
                    return gestoreRepositoryCall.patchRichiestaEvent(notificationTrackerQueueDto.getRequestIdx(),
                            new EventsDto().digProgrStatus(digitalProgressStatusDto)
                                    .paperProgrStatus(paperProgressStatusDto));
                })
                .filter(objects -> objects.getT1().getLogicStatus() != null)
                .flatMap(objects -> {

                    var lastEventIndex = objects.getT2().getRequestMetadata().getEventsList().size() - 1;

                    if (notificationTrackerQueueDto.getDigitalProgressStatusDto() != null) {

                        var digitalMessageProgressEvent = new BaseMessageProgressEvent();
                        digitalMessageProgressEvent.setRequestId(objects.getT2().getRequestIdx());
                        var lastEventUpdatedDigital = objects.getT2().getRequestMetadata().getEventsList().get(lastEventIndex).getDigProgrStatus();
                        digitalMessageProgressEvent.setEventTimestamp(lastEventUpdatedDigital.getEventTimestamp());
                        var status = Enum.valueOf(ProgressEventCategory.class, objects.getT1().getExternalStatus());
                        digitalMessageProgressEvent.setStatus(status);
                        digitalMessageProgressEvent.setEventCode(objects.getT1().getLogicStatus());
                        digitalMessageProgressEvent.setEventDetails(lastEventUpdatedDigital.getEventDetails());

                        var generatedMessage = lastEventUpdatedDigital.getGeneratedMessage();
                        var digitalMessageReference = new DigitalMessageReference();
                        digitalMessageReference.setId(generatedMessage.getId());
                        digitalMessageReference.setSystem(generatedMessage.getSystem());
                        digitalMessageReference.setLocation(generatedMessage.getLocation());
                        digitalMessageProgressEvent.setGeneratedMessage(digitalMessageReference);

                        return putEvents.putEventExternal(digitalMessageProgressEvent, notificationTrackerQueueDto.getProcessId());

                    } else {

                        var paperProgressEvent = new PaperProgressStatusEvent();
                        paperProgressEvent.setRequestId(objects.getT2().getRequestIdx());
                        var lastEventUpdatedPaper = objects.getT2().getRequestMetadata().getEventsList().get(lastEventIndex).getPaperProgrStatus();
                        paperProgressEvent.setRegisteredLetterCode(lastEventUpdatedPaper.getRegisteredLetterCode());
                        var paperRequest = objects.getT2().getRequestMetadata().getPaperRequestMetadata();
                        paperProgressEvent.setProductType(paperRequest.getProductType());
                        paperProgressEvent.setIun(paperRequest.getIun());
                        paperProgressEvent.setStatusCode(objects.getT1().getLogicStatus());
                        // TODO check se va bene per il cartaceo questo stato esterno
                        paperProgressEvent.setStatusDescription(objects.getT1().getExternalStatus());
                        paperProgressEvent.setStatusDateTime(lastEventUpdatedPaper.getStatusDateTime());
                        paperProgressEvent.setDeliveryFailureCause(lastEventUpdatedPaper.getDeliveryFailureCause());

                        var attachmentsDetails = new AttachmentDetails();
                        var attachmentsDetailsList = new ArrayList<AttachmentDetails>();

                        List<AttachmentsProgressEventDto> attachmentsProgressEventDtolist = lastEventUpdatedPaper.getAttachments();

                        for (AttachmentsProgressEventDto attachmentsProgressEventDto : attachmentsProgressEventDtolist) {
                            attachmentsDetails.setId(attachmentsProgressEventDto.getId());
                            attachmentsDetails.setDocumentType(attachmentsProgressEventDto.getDocumentType());
                            attachmentsDetails.setUrl(attachmentsProgressEventDto.getUri());
                            attachmentsDetails.setDate(attachmentsProgressEventDto.getDate());
                            attachmentsDetailsList.add(attachmentsDetails);
                        }
                        paperProgressEvent.setAttachments(attachmentsDetailsList);

                        var lastDiscoveredAddress = lastEventUpdatedPaper.getDiscoveredAddress();
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
                        paperProgressEvent.setDiscoveredAddress(discoveredAddress);

                        paperProgressEvent.setClientRequestTimeStamp(objects.getT2().getRequestTimeStamp());

                        return putEvents.putEventExternal(paperProgressEvent, notificationTrackerQueueDto.getProcessId());
                    }
                })
                .doOnSuccess(result -> acknowledgment.acknowledge())
                .then()
                .doOnError(InvalidNextStatusException.class, throwable -> acknowledgment.acknowledge())
                .onErrorResume(InvalidNextStatusException.class, e -> {
                    var retry = notificationTrackerQueueDto.getRetry();
                    notificationTrackerQueueDto.setRetry(retry + 1);
                    if (retry < 5) {
                        return sqsService.send(ntStatoQueueName, notificationTrackerQueueDto).then();
                    } else {
                        //TODO: DLQ
                        return sqsService.send(ntStatoDlQueueName, notificationTrackerQueueDto).then();
                    }
                });
    }
}
