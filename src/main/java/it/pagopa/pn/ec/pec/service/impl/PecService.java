package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.uribuilder.UriBuilderCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pec.model.dto.NtStatoPecQueueDto;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_PEC;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;

@Service
@Slf4j
public class PecService extends PresaInCaricoService {

    private final SqsService sqsService;

    private final UriBuilderCall uriBuilderCall;

    protected PecService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                         UriBuilderCall uriBuilderCall) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.uriBuilderCall = uriBuilderCall;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo, RequestDto requestToInsert) {
//      Cast PresaInCaricoInfo to specific SmsPresaInCaricoInfo
        PecPresaInCaricoInfo pecPresaInCaricoInfo = (PecPresaInCaricoInfo) presaInCaricoInfo;
        return uriBuilderCall.getFile(pecPresaInCaricoInfo.getDigitalNotificationRequest().getAttachmentsUrls().get(0),
                                      presaInCaricoInfo.getXPagopaExtchCxId(),
                                      false)
                             .flatMap(fileDownloadResponse -> {
                                 var digitalNotificationRequest = pecPresaInCaricoInfo.getDigitalNotificationRequest();
                                 digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                                 return insertRequestFromPec(digitalNotificationRequest).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                             })
                             .flatMap(requestDto -> sqsService.send(NT_STATO_PEC_QUEUE_NAME,
                                                                    new NtStatoPecQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                           INVIO_PEC,
                                                                                           null,
                                                                                           BOOKED)))
                             .flatMap(fileDownloadResponse -> sqsService.send(NT_STATO_PEC_QUEUE_NAME,
                                                                              new NtStatoPecQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                                                                                     INVIO_PEC,
                                                                                                     null,
                                                                                                     BOOKED)))
                             .flatMap(sendMessageResponse -> {
                                 DigitalNotificationRequest.QosEnum qos = pecPresaInCaricoInfo.getDigitalNotificationRequest().getQos();
                                 if (qos == INTERACTIVE) {
                                     return sqsService.send(PEC_INTERACTIVE_QUEUE_NAME,
                                                            pecPresaInCaricoInfo.getDigitalNotificationRequest());
                                 } else if (qos == BATCH) {
                                     return sqsService.send(PEC_BATCH_QUEUE_NAME, pecPresaInCaricoInfo.getDigitalNotificationRequest());
                                 } else {
                                     return Mono.empty();
                                 }
                             })
                             .then();
    }

    private Mono<RequestDto> insertRequestFromPec(final DigitalNotificationRequest digitalNotificationRequest) {
        return Mono.just(new RequestDto());
    }
}
