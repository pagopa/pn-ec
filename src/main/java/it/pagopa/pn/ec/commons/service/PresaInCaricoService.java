package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@SuppressWarnings("unused")
public abstract class PresaInCaricoService {

    private final AuthService authService;
    private final GestoreRepositoryCall gestoreRepositoryCall;

    protected PresaInCaricoService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall) {
        this.authService = authService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    public Mono<Void> presaInCarico(PresaInCaricoInfo presaInCaricoInfo) throws RequestAlreadyInProgressException {
        return authService.clientAuth(presaInCaricoInfo.getXPagopaExtchCxId())
                          .then(gestoreRepositoryCall.getRichiesta(presaInCaricoInfo.getRequestIdx()))
                          .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                         throwable -> {
                                             log.info("The request with id {} doesn't exist", presaInCaricoInfo.getRequestIdx());
                                             return Mono.empty();
                                         })
                          .handle((existingRequest, sink) -> {
                              if (existingRequest != null) {
                                  sink.error(new RequestAlreadyInProgressException(presaInCaricoInfo.getRequestIdx()));
                              }
                          })
                          .then(specificPresaInCarico(presaInCaricoInfo));
    }

    protected abstract Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo);
}
