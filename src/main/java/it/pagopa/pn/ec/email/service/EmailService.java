package it.pagopa.pn.ec.email.service;



import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;

import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.attachments.CheckAttachments;
import it.pagopa.pn.ec.email.model.dto.NtStatoEmailQueueDto;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.ec.commons.constant.ProcessId.*;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.ChannelEnum.EMAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestDto.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.BOOKED;


@Service
@Slf4j
public class EmailService  extends PresaInCaricoService {

    private final SqsService sqsService;

    private final GestoreRepositoryCall gestoreRepositoryCall;

    private final CheckAttachments checkAttachments;

    protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                           GestoreRepositoryCall gestoreRepositoryCall1, CheckAttachments checkAttachments) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.checkAttachments = checkAttachments;
    }



    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
        EmailPresaInCaricoInfo emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;

        return checkAttachments.checkAllegatiPresence(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getAttachmentsUrls(),
                        presaInCaricoInfo.getXPagopaExtchCxId(),
                        false).flatMap(fileDownloadResponse -> {
                    var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
                    digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
                    return insertRequestFromEmail(digitalNotificationRequest).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
                })
                .flatMap(requestDto -> sqsService.send(NT_STATO_EMAIL_QUEUE_NAME,
                        new NtStatoEmailQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                INVIO_MAIL,
                                null,
                                BOOKED.getValue())))
                .flatMap(fileDownloadResponse -> sqsService.send(NT_STATO_EMAIL_QUEUE_NAME,
                        new NtStatoEmailQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                INVIO_MAIL,
                                null,
                                BOOKED.getValue())))
                .flatMap(sendMessageResponse -> {
                    DigitalCourtesyMailRequest.QosEnum qos = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest()
                            .getQos();
                    if (qos == INTERACTIVE) {
                        return sqsService.send(EMAIL_INTERACTIVE_QUEUE_NAME,
                                emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
                    } else if (qos == BATCH) {
                        return sqsService.send(EMAIL_BATCH_QUEUE_NAME,
                                emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
                    } else {
                        return Mono.empty();
                    }
                })
                .then();
    }

    private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(digitalCourtesyMailRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(digitalCourtesyMailRequest.getClientRequestTimeStamp());
            var digitalRequestDto = new DigitalRequestDto();
            digitalRequestDto.setCorrelationId(digitalCourtesyMailRequest.getCorrelationId());
            digitalRequestDto.setEventType("");
            digitalRequestDto.setQos(DigitalRequestDto.QosEnum.valueOf(digitalCourtesyMailRequest.getQos().name()));
            digitalRequestDto.setTags(digitalCourtesyMailRequest.getTags());
            digitalRequestDto.setReceiverDigitalAddress(digitalCourtesyMailRequest.getReceiverDigitalAddress());
            digitalRequestDto.setMessageText(digitalCourtesyMailRequest.getMessageText());
            digitalRequestDto.setSenderDigitalAddress(digitalCourtesyMailRequest.getSenderDigitalAddress());
            digitalRequestDto.setChannel(EMAIL);
            digitalRequestDto.setSubjectText("");
            digitalRequestDto.setMessageContentType(PLAIN);
            requestDto.setDigitalReq(digitalRequestDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }


}
