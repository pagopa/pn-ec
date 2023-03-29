package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException.IdClientNotFoundException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final GestoreRepositoryCall gestoreRepositoryCall;

    public AuthServiceImpl(GestoreRepositoryCall gestoreRepositoryCall) {
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    @Override
    public Mono<Void> clientAuth(final String xPagopaExtchCxId) throws IdClientNotFoundException {
        log.info("<-- START CLIENT AUTHORIZATION --> Client ID: {}", xPagopaExtchCxId);
        return gestoreRepositoryCall.getClientConfiguration(xPagopaExtchCxId)
                                    .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                                   throwable -> Mono.error(new ClientNotFoundException(xPagopaExtchCxId)))
                                    .then();
    }
}
