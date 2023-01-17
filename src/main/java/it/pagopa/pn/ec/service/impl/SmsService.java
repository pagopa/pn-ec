package it.pagopa.pn.ec.service.impl;

import it.pagopa.pn.ec.model.dto.NotTrackPresaInCaricoSmsDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import it.pagopa.pn.ec.service.AuthService;
import it.pagopa.pn.ec.service.InvioService;
import it.pagopa.pn.ec.service.SqsService;
import org.springframework.stereotype.Service;

@Service
public class SmsService extends InvioService {

    private final SqsService sqsService;

    protected SmsService(AuthService authService, SqsService sqsService) {
        super(authService);
        this.sqsService = sqsService;
    }

    @Override
    public void presaInCarico(String idClient, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super.presaInCarico(idClient, digitalCourtesySmsRequest);

        // TODO: Retrieve current status from "Gestore Repository"
        NotTrackPresaInCaricoSmsDto notTrackPresaInCaricoSmsDto = new NotTrackPresaInCaricoSmsDto(idClient, "", digitalCourtesySmsRequest);
        sqsService.send("", notTrackPresaInCaricoSmsDto);

        // TODO: Send to "SMS" queue
    }

    @Override
    public void lavorazioneRichiesta() {
        // TODO: Implement lavorazione richiesta invio SMS
    }

    @Override
    public void retry() {
        // TODO: Implement retry invio SMS
    }
}
