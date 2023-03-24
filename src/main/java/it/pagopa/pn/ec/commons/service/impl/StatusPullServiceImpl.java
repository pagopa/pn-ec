package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.exception.StatusNotFoundException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Service
public class StatusPullServiceImpl implements StatusPullService {

    private final AuthService authService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final CallMacchinaStati callMacchinaStati;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public StatusPullServiceImpl(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall,
                                 CallMacchinaStati callMacchinaStati,
                                 TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.authService = authService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        this.callMacchinaStati = callMacchinaStati;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    public Mono<CourtesyMessageProgressEvent> digitalPullService(String requestIdx, String xPagopaExtchCxId, String processId) {
        return getRequest(xPagopaExtchCxId, requestIdx)
                .flatMap(requestDto -> getLastEvent(requestDto))
                .flatMap(eventDTO -> {
                    var event = new CourtesyMessageProgressEvent();
                    var digProgrStatus = eventDTO.getDigProgrStatus();

                    event.setRequestId(requestIdx);
                    event.setEventDetails(digProgrStatus.getEventDetails());
                    event.setEventTimestamp(digProgrStatus.getEventTimestamp());

                    var generatedMessageDTO = digProgrStatus.getGeneratedMessage();
                    if (generatedMessageDTO != null) {
                        var digitalMessageReference = new DigitalMessageReference();

                        digitalMessageReference.setId(generatedMessageDTO.getId());
                        digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
                        digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

                        event.setGeneratedMessage(digitalMessageReference);
                    }
                    return statusDecode(RequestStatusChange.builder()
                            .xPagopaExtchCxId(xPagopaExtchCxId)
                            .currentStatus(digProgrStatus.getStatus().toLowerCase())
                            .processId(processId)
                            .build()).map(macchinaStatiDecodeResponseDto -> {
                        event.setStatus(ProgressEventCategory.valueOf(macchinaStatiDecodeResponseDto.getExternalStatus()));
                        event.setEventCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                        return event;
                    });
                }).switchIfEmpty(Mono.just(new CourtesyMessageProgressEvent().eventCode("").eventDetails("").requestId("")));

    }

    @Override
    public Mono<LegalMessageSentDetails> pecPullService(String requestIdx, String xPagopaExtchCxId) {
        return getRequest(xPagopaExtchCxId, requestIdx)
                .flatMap(requestDto -> getLastEvent(requestDto))
                .flatMap(eventDTO -> {
                    var event = new LegalMessageSentDetails();
                    var digProgrStatus = eventDTO.getDigProgrStatus();

                    event.setRequestId(requestIdx);
                    event.setEventDetails(digProgrStatus.getEventDetails());
                    event.setEventTimestamp(digProgrStatus.getEventTimestamp());

                    var generatedMessageDTO = digProgrStatus.getGeneratedMessage();
                    if (generatedMessageDTO != null) {
                        var digitalMessageReference = new DigitalMessageReference();

                        digitalMessageReference.setId(generatedMessageDTO.getId());
                        digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
                        digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

                        event.setGeneratedMessage(digitalMessageReference);
                    }
                    return statusDecode(RequestStatusChange.builder()
                            .xPagopaExtchCxId(xPagopaExtchCxId)
                            .currentStatus(digProgrStatus.getStatus().toLowerCase())
                            .processId(transactionProcessConfigurationProperties.pec())
                            .build()).map(statiDecodeResponseDto -> {
                        if (statiDecodeResponseDto.getExternalStatus() != null) {
                            event.setStatus(ProgressEventCategory.valueOf(statiDecodeResponseDto.getExternalStatus()));
                            event.setEventCode(statiDecodeResponseDto.getLogicStatus());
                        }
                        return event;
                    });
                }).switchIfEmpty(Mono.just(new LegalMessageSentDetails().eventCode("").eventDetails("").requestId("")));
    }

    @Override
    public Mono<PaperProgressStatusEvent> paperPullService(String requestIdx, String xPagopaExtchCxId) {
        return getRequest(xPagopaExtchCxId, requestIdx).flatMap(requestDto -> {

                    var eventsList = requestDto.getRequestMetadata().getEventsList();

                    if (eventsList != null && !eventsList.isEmpty()) {

                        var lastIndex = requestDto.getRequestMetadata().getEventsList().size() - 1;
                        var lastEventUpdated = requestDto.getRequestMetadata().getEventsList().get(lastIndex);

                        var event = new PaperProgressStatusEvent();
                        var paperProgrStatus = lastEventUpdated.getPaperProgrStatus();

                        event.setRequestId(requestIdx);

                        event.setStatusDateTime(paperProgrStatus.getStatusDateTime());
                        event.setDeliveryFailureCause(paperProgrStatus.getDeliveryFailureCause());
                        event.setRegisteredLetterCode(paperProgrStatus.getRegisteredLetterCode());

                        var discoveredAddress = new DiscoveredAddress();
                        var discoveredAddressDTO = paperProgrStatus.getDiscoveredAddress();

                        if (discoveredAddressDTO != null) {
                            discoveredAddress.setAddress(discoveredAddressDTO.getAddress());
                            discoveredAddress.setAddressRow2(discoveredAddressDTO.getAddressRow2());
                            discoveredAddress.setCap(discoveredAddressDTO.getCap());
                            discoveredAddress.setCity(discoveredAddressDTO.getCity());
                            discoveredAddress.setCity2(discoveredAddressDTO.getCity2());
                            discoveredAddress.setCountry(discoveredAddressDTO.getCountry());
                            discoveredAddress.setName(discoveredAddressDTO.getName());
                            discoveredAddress.setNameRow2(discoveredAddressDTO.getNameRow2());
                            discoveredAddress.setPr(discoveredAddressDTO.getPr());
                        }
                        event.setDiscoveredAddress(discoveredAddress);

                        // Settiamo all'evento lo status NON ANCORA decodificato. La decodifica
                        // avverrà
                        // successivamente.
                        event.setStatusDescription(paperProgrStatus.getStatusDescription());

                        var attachmentsListDTO = paperProgrStatus.getAttachments();
                        var attachmentList = new ArrayList<AttachmentDetails>();

                        if (attachmentsListDTO != null) {
                            for (AttachmentsProgressEventDto attachmentDTO : attachmentsListDTO) {

                                var attachment = new AttachmentDetails();

                                attachment.setDate(attachmentDTO.getDate());
                                attachment.setDocumentType(attachmentDTO.getDocumentType());
                                attachment.setId(attachmentDTO.getId());
                                attachment.setUrl(attachmentDTO.getUri());

                                attachmentList.add(attachment);
                            }
                        }
                        event.setAttachments(attachmentList);

                        event.setClientRequestTimeStamp(requestDto.getClientRequestTimeStamp());
                        event.setIun(requestDto.getRequestMetadata().getPaperRequestMetadata().getIun());
                        event.setProductType(requestDto.getRequestMetadata().getPaperRequestMetadata().getProductType());

                        return Mono.just(event);
                    } else {
                        return Mono.empty();
                    }
                }).flatMap(event -> {

                    // Decodifica dello stato della richiesta.
                    return statusDecode(RequestStatusChange.builder()
                            .xPagopaExtchCxId(xPagopaExtchCxId)
                            .currentStatus(event.getStatusDescription())
                            .processId(transactionProcessConfigurationProperties.paper())
                            .build()).map(macchinaStatiDecodeResponseDto -> {
                        event.setStatusDescription(macchinaStatiDecodeResponseDto.getExternalStatus());
                        event.setStatusCode(macchinaStatiDecodeResponseDto.getLogicStatus());
                        return event;
                    });
                })

                .switchIfEmpty(Mono.just(new PaperProgressStatusEvent().requestId(requestIdx)
                        .statusDescription("")
                        .deliveryFailureCause("")
                        .productType("")
                        .statusCode("")
                        .statusDescription("")
                        .iun("")
                        .registeredLetterCode("")));

    }

    private Mono<MacchinaStatiDecodeResponseDto> statusDecode(RequestStatusChange requestStatusChange) {
        return callMacchinaStati.statusDecode(requestStatusChange).handle((macchinaStatiDecodeResponseDto, sink) -> {
            if (macchinaStatiDecodeResponseDto.getExternalStatus() == null)
                sink.error(new StatusNotFoundException(requestStatusChange.getCurrentStatus()));
            else sink.next(macchinaStatiDecodeResponseDto);
        });
    }

    private Mono<RequestDto> getRequest(String xPagopaExtchCxId, String requestIdx) {
        return authService.clientAuth(xPagopaExtchCxId)
                          .then(gestoreRepositoryCall.getRichiesta(xPagopaExtchCxId, requestIdx))
                          .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                         e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
                          .handle((requestDto, synchronousSink) -> {
                              String requestClientID = requestDto.getxPagopaExtchCxId();
                              if (requestClientID == null || !requestClientID.equals(xPagopaExtchCxId)) {
                                  synchronousSink.error(new ClientNotAuthorizedException(xPagopaExtchCxId));
                              } else {
                                  synchronousSink.next(requestDto);
                              }

                          });
    }

    private Mono<EventsDto> getLastEvent(RequestDto requestDto) {
        var eventsList = requestDto.getRequestMetadata().getEventsList();
        if (eventsList != null && !eventsList.isEmpty()) {
            var lastIndex = requestDto.getRequestMetadata().getEventsList().size()-1;
            var lastEventUpdated = requestDto.getRequestMetadata().getEventsList().get(lastIndex);
            return Mono.just(lastEventUpdated);
        } else {
            return Mono.empty();
        }
    }
}
