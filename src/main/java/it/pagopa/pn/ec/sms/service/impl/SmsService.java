package it.pagopa.pn.ec.sms.service.impl;

import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.RequestBaseInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCallImpl;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.InvioService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.sms.model.pojo.SmsRequestBaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;

@Service
@Slf4j
public class SmsService extends InvioService {

    private final SqsService sqsService;

    protected SmsService(AuthService authService, RichiesteCallImpl gestoreRepositoryCall, SqsService sqsService) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final RequestBaseInfo requestBaseInfo) {
        log.info("<-- Start presa in carico SMS-->");

        // Cast base object invioBaseRequest for the specific case
        var invioSmsDto = (SmsRequestBaseInfo) requestBaseInfo;

        // Preparation of the DTO and sending to the "Notification Tracker stato SMS" queue
        return sqsService.send(NT_STATO_SMS_QUEUE_NAME, new NotificationTrackerQueueDto(invioSmsDto, null, BOOKED))
                         // Send to "SMS" queue
                         .then(sqsService.send(SMS_QUEUE_NAME, invioSmsDto.getDigitalCourtesySmsRequest()));
    }

    @Override
    public Mono<Void> lavorazioneRichiesta() {
        return null;
    }

    @Override
    public Mono<Void> retry() {
        return Mono.empty();
    }
}
