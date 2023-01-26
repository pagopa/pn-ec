package it.pagopa.pn.ec.sms.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCallImpl;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.InvioService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.SMS_QUEUE_NAME;

@Service
@Slf4j
public class SmsService extends InvioService {

    private final SqsService sqsService;

    protected SmsService(AuthService authService, RichiesteCallImpl gestoreRepositoryCall, SqsService sqsService) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final Status status, final PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- Start presa in carico SMS-->");

        // Cast base object invioBaseRequest for the specific case
        var invioSmsDto = (SmsPresaInCaricoInfo) presaInCaricoInfo;

        // Preparation of the DTO and sending to the "Notification Tracker stato SMS" queue
        return sqsService.send(NT_STATO_SMS_QUEUE_NAME, new NtStatoSmsQueueDto(presaInCaricoInfo, status))
                         // Send to "SMS" queue
                         .then(sqsService.send(SMS_QUEUE_NAME, invioSmsDto.getDigitalCourtesySmsRequest()));
    }

    @Override
    @SqsListener(value = SMS_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneRichiesta(final DigitalCourtesySmsRequest digitalCourtesySmsRequest, final Acknowledgment acknowledgment) {
        sqsService.incomingMessageFlow(digitalCourtesySmsRequest, acknowledgment).subscribe();
    }

    @Override
    public void retry() {

    }
}
