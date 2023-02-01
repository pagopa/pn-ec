package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.*;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService {

    private final SqsService sqsService;
    private final SnsService snsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;

    protected SmsService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                         SnsService snsService, GestoreRepositoryCall gestoreRepositoryCall1) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
        return sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                               new NtStatoSmsQueueDto(presaInCaricoInfo.getXPagopaExtchCxId(), INVIO_SMS, null, BOOKED))
                         .map(unused -> (SmsPresaInCaricoInfo) presaInCaricoInfo)
                         .flatMap(smsPresaInCaricoInfo -> {
                             DigitalCourtesySmsRequest.QosEnum qos = smsPresaInCaricoInfo.getDigitalCourtesySmsRequest().getQos();
                             if (qos == INTERACTIVE) {
                                 return sqsService.send(SMS_INTERACTIVE_QUEUE_NAME, smsPresaInCaricoInfo.getDigitalCourtesySmsRequest());
                             } else if (qos == BATCH) {
                                 return sqsService.send(SMS_BATCH_QUEUE_NAME, smsPresaInCaricoInfo.getDigitalCourtesySmsRequest());
                             } else {
                                 return Mono.empty();
                             }
                         })
                         .then();
    }

    @SqsListener(value = SMS_INTERACTIVE_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void lavorazioneRichiesta(final DigitalCourtesySmsRequest digitalCourtesySmsRequest) {

        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        log.info("Incoming message from '{}' queue", SMS_INTERACTIVE_QUEUE_NAME);

        String requestIdx = digitalCourtesySmsRequest.getRequestId();

        Mono.just(digitalCourtesySmsRequest)
            .doOnNext(message -> log.info("Incoming message {}", message))
//          Try to send SMS
            .then(snsService.send(digitalCourtesySmsRequest.getMessageText(), digitalCourtesySmsRequest.getReceiverDigitalAddress()))
//          Retrive request
            .flatMap(publishResponse -> gestoreRepositoryCall.getRichiesta(requestIdx))
//          Send to Notification Tracker with next status -> SENT
            .flatMap(requestDto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME,
                                                   new NotificationTrackerQueueDto(requestIdx, null, INVIO_SMS, BOOKED, SENT)))
//          An error occurred during SMS send, the retries are started, send to Notification Tracker with next status -> RETRY
//            .onErrorResume(SnsSendException.class,
//                           snsMaxRetriesExceeded -> Mono.just(new NotificationTrackerQueueDto(requestIdx, null, INVIO_SMS, BOOKED, RETRY))
//                                                        .flatMap(dto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME, dto)))
//          The short retries are finished, send to Notification Tracker with next status -> ERROR
            .onErrorResume(SnsSendException.SnsMaxRetriesExceededException.class,
                           snsMaxRetriesExceeded -> Mono.just(new NotificationTrackerQueueDto(requestIdx, null, INVIO_SMS, RETRY, ERROR))
                                                        .flatMap(dto -> sqsService.send(NT_STATO_SMS_QUEUE_NAME, dto)))
            .subscribe();
    }
}
