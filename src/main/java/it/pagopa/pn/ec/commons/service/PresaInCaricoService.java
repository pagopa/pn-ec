package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
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
//      Client auth
        return authService.clientAuth(presaInCaricoInfo.getXPagopaExtchCxId())
//                        Retrieve request
                          .then(gestoreRepositoryCall.getRichiesta(presaInCaricoInfo.getRequestIdx()))
//                        The request exists
//                        TODO: Definire la logica di una richiesta già presente. Al momento se una richiesta esiste viene tornato
//                         direttamente un 409 come risposta
                          .handle((existingRequest, sink) -> sink.error(new RequestAlreadyInProgressException(existingRequest.getRequestIdx())))
//                        The request doesn't exist
                          .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
                              log.debug("The request with id {} doesn't exist", presaInCaricoInfo.getRequestIdx());
                              return specificPresaInCarico(presaInCaricoInfo);
                          }).then();
    }

    protected abstract Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo);
}
