package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.Status.IN_LAVORAZIONE;

@Service
@Slf4j
@SuppressWarnings("unused")
public abstract class InvioService {

    private final AuthService authService;
    private final RichiesteCall richiesteCall;

    protected InvioService(AuthService authService, RichiesteCall richiesteCall) {
        this.authService = authService;
        this.richiesteCall = richiesteCall;
    }

    public Mono<Void> presaInCarico(final PresaInCaricoInfo presaInCaricoInfo) throws RequestAlreadyInProgressException {
        log.info("<-- Start presa in carico -->");
        return authService.clientAuth(presaInCaricoInfo.getIdClient())
                          .then(richiesteCall.getRichiesta(presaInCaricoInfo.getIdRequest()))
                          .flatMap(status -> {
                              if (status == IN_LAVORAZIONE)
                                  return Mono.error(new RequestAlreadyInProgressException(presaInCaricoInfo.getIdRequest()));
                              else return specificPresaInCarico(status, presaInCaricoInfo);
                          })
                          .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                          .then();
    }

    protected abstract Mono<Void> specificPresaInCarico(final Status status, final PresaInCaricoInfo presaInCaricoInfo);

    public abstract Mono<Void> lavorazioneRichiesta();

    public abstract Mono<Void> retry();
}
