package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCallImpl;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.InvioService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pec.model.dto.NtStatoPecQueueDto;
import it.pagopa.pn.ec.pec.model.pojo.PecPresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.NT_STATO_PEC_QUEUE_NAME;
import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.PEC_QUEUE_NAME;

@Service
@Slf4j
public class PecService extends InvioService {

    private final SqsService sqsService;

    protected PecService(AuthService authService, RichiesteCallImpl gestoreRepositoryCall, SqsService sqsService) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(final Status status, final PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- Start presa in carico PEC-->");

        // Cast base object invioBaseRequest for the specific case
        var invioPecDto = (PecPresaInCaricoInfo) presaInCaricoInfo;

        // Preparation of the DTO and sending to the "Notification Tracker stato PEC" queue
        return sqsService.send(NT_STATO_PEC_QUEUE_NAME, new NtStatoPecQueueDto(presaInCaricoInfo, status))
                // Send to "PEC" queue
                .then(sqsService.send(PEC_QUEUE_NAME, invioPecDto.getDigitalNotificationRequest()));
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
