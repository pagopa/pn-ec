package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest.QosEnum.INTERACTIVE;

@Service
@Slf4j
public class SmsService extends PresaInCaricoService {

    private final SqsService sqsService;

    protected SmsService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
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

    @SqsListener(value = SMS_INTERACTIVE_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    @ConditionalOnProperty(name = "test.aws.sqs.disable-listener", havingValue = "true", matchIfMissing = true)
    public void lavorazioneRichiesta(final DigitalCourtesySmsRequest digitalCourtesySmsRequest, final Acknowledgment acknowledgment) {
        sqsService.incomingMessageFlow(digitalCourtesySmsRequest, acknowledgment).subscribe();
    }
}
