package it.pagopa.pn.ec.email.service;


import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.InvioService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.model.dto.NtStatoSmsQueueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.QueueNameConstant.*;

@Service
@Slf4j
public class EmailService  extends InvioService {

    private final SqsService sqsService;

    protected EmailService(AuthService authService, RichiesteCall richiesteCall, SqsService sqsService) {
        super(authService, richiesteCall);
        this.sqsService = sqsService;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(Status status, PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- Start presa in carico EMAIL-->");

        // Cast base object invioBaseRequest for the specific case
        var invioemailDto = (EmailPresaInCaricoInfo) presaInCaricoInfo;

        // Preparation of the DTO and sending to the "Notification Tracker stato Email" queue
        return sqsService.send(NT_STATO_EMAIL_QUEUE_NAME, new NtStatoSmsQueueDto(presaInCaricoInfo, status))
                // Send to "EMAIL" queue
                .then(sqsService.send(EMAIL_QUEUE_NAME, invioemailDto.getDigitalCourtesyMailRequest()));
    }

    @Override
    public Mono<Void> lavorazioneRichiesta() {
        return null;
    }

    @Override
    public Mono<Void> retry() {
        return null;
    }
}
