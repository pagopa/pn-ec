package it.pagopa.pn.ec.sms.service.impl;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.InvioService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_SMS;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.SMS_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.status.CommonStatus.BOOKED;

@Service
@Slf4j
public class SmsService extends InvioService {

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
                         .flatMap(smsPresaInCaricoInfo -> sqsService.send(SMS_QUEUE_NAME,
                                                                          smsPresaInCaricoInfo.getDigitalCourtesySmsRequest()));
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
