package it.pagopa.pn.ec.service.impl;

import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.model.dto.NotTrackQueueSmsDto;
import it.pagopa.pn.ec.model.dto.SmsQueueDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.service.AuthService;
import it.pagopa.pn.ec.service.InvioService;
import it.pagopa.pn.ec.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static it.pagopa.pn.ec.constant.QueueNameConstant.NOTIFICATION_TRACKER_QUEUE_NAME;
import static it.pagopa.pn.ec.constant.QueueNameConstant.SMS_QUEUE_NAME;

@Service
@Slf4j
public class SmsService extends InvioService {

    private final SqsService sqsService;

    protected SmsService(AuthService authService, SqsService sqsService) {
        super(authService);
        this.sqsService = sqsService;
    }

    @Override
    public void presaInCarico(String idClient, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super.presaInCarico(idClient, digitalCourtesySmsRequest);

        // TODO: Retrieve the current status from "Gestore Repository" and set it in the DTO

        // Preparation of the DTO and sending to the "Notification Tracker" queue
        sqsService.send(NOTIFICATION_TRACKER_QUEUE_NAME, new NotTrackQueueSmsDto(idClient, "", digitalCourtesySmsRequest));

        // Send to "SMS" queue
        // TODO: DTO to be defined
        sqsService.send(SMS_QUEUE_NAME,
                        new SmsQueueDto(digitalCourtesySmsRequest.getSenderDigitalAddress(), digitalCourtesySmsRequest.getMessageText()));
    }

    @Override
    public void lavorazioneRichiesta() {
        // TODO: Implement lavorazione richiesta invio SMS
    }

    /**
     * Metodo di prova per SQS listener
     * @param notTrackQueueSmsDto
     * Oggetto che torna dalla coda
     */
    @SqsListener(value = NOTIFICATION_TRACKER_QUEUE_NAME, deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void lavorazioneRichiesta(final NotTrackQueueSmsDto notTrackQueueSmsDto) {
        log.info("Received message from {} queue -> {}", NOTIFICATION_TRACKER_QUEUE_NAME, notTrackQueueSmsDto);
    }

    @Override
    public void retry() {
        // TODO: Implement retry invio SMS
    }
}
