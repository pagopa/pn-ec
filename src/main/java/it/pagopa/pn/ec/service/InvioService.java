package it.pagopa.pn.ec.service;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("unused")
public abstract class InvioService {

    private final AuthService authService;

    protected InvioService(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Presa in carico di un SMS
     *
     * @param idClient                  Client id da autenticare
     * @param digitalCourtesySmsRequest Body della request per l'invio di un SMS che verrà passato alla coda "Notification Tracker"
     */
    public void presaInCarico(String idClient, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        authService.checkIdClient(idClient);
    }

    /**
     * Presa in carico di una MAIL o di una PEC senza valore legale
     *
     * @param idClient                   Client id da autenticare
     * @param digitalCourtesyMailRequest Body della request per l'invio di una MAIL o di una PEC senza valore legale che verrà passato
     *                                   alla coda "Notification Tracker"
     */
    public void presaInCarico(String idClient, DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
        authService.checkIdClient(idClient);
    }

    public abstract void lavorazioneRichiesta();

    public abstract void retry();
}
