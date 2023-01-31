package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@SuppressWarnings("unused")
public abstract class InvioService {

    private final AuthService authService;
    private final GestoreRepositoryCall gestoreRepositoryCall;

    protected InvioService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall) {
        this.authService = authService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    public Mono<Void> presaInCarico(PresaInCaricoInfo presaInCaricoInfo) throws RequestAlreadyInProgressException {
        log.info("<-- Start presa in carico -->");
        return authService.clientAuth(presaInCaricoInfo.getXPagopaExtchCxId())
                          .then(gestoreRepositoryCall.getRichiesta(presaInCaricoInfo.getRequestIdx()))
                          .flatMap(existingRequest -> {
                              if (existingRequest != null)
                                  return Mono.error(new RequestAlreadyInProgressException(presaInCaricoInfo.getRequestIdx()));
                              else return specificPresaInCarico(presaInCaricoInfo);
                          })
                          .doOnError(throwable -> log.error(throwable.getMessage(), throwable))
                          .then();
    }

    protected abstract Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo);

    public abstract Mono<Void> lavorazioneRichiesta();

    public abstract Mono<Void> retry();
}
