package it.pagopa.pn.ec.email.service;



import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.uribuilder.UriBuilderCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.attachments.CheckAttachments;
import it.pagopa.pn.ec.email.model.dto.NtStatoEmailQueueDto;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.ec.commons.constant.ProcessId.*;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;


@Service
@Slf4j
public class EmailService  extends PresaInCaricoService {

    private final SqsService sqsService;

    private final UriBuilderCall uriBuilderCall;

    private final CheckAttachments checkAttachments;

    protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                           UriBuilderCall uriBuilderCall,  CheckAttachments checkAttachments) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.uriBuilderCall = uriBuilderCall;
        this.checkAttachments = checkAttachments;
    }



    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo, RequestDto requestToInsert) {
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
                                BOOKED)))
                .flatMap(fileDownloadResponse -> sqsService.send(NT_STATO_EMAIL_QUEUE_NAME,
                        new NtStatoEmailQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(),
                                INVIO_MAIL,
                                null,
                                BOOKED)))
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
        return Mono.just(new RequestDto());
    }

}
