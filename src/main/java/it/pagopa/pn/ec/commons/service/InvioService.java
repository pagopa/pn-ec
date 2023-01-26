package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.RequestBaseInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste.RichiesteCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


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

    public Mono<Void> presaInCarico(final RequestBaseInfo requestBaseInfo) throws RequestAlreadyInProgressException {
        log.info("<-- Start presa in carico -->");
        return authService.clientAuth(requestBaseInfo.getIdClient())
                          .then(richiesteCall.getRichiesta(requestBaseInfo.getIdRequest()))
                          .flatMap(currentStatus -> {
                              if (currentStatus != null)
                                  return Mono.error(new RequestAlreadyInProgressException(requestBaseInfo.getIdRequest()));
                              else return specificPresaInCarico(requestBaseInfo);
                          })
                          .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                          .then();
    }

    protected abstract Mono<Void> specificPresaInCarico(final RequestBaseInfo requestBaseInfo);

    public abstract Mono<Void> lavorazioneRichiesta();

    public abstract Mono<Void> retry();
}
