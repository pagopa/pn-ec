package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException.IdClientNotFoundException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final GestoreRepositoryCall gestoreRepositoryCall;

    public AuthServiceImpl(GestoreRepositoryCall gestoreRepositoryCall) {
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    @Override
    public Mono<ClientConfigurationInternalDto> clientAuth(final String xPagopaExtchCxId) {
        log.info("<-- START CLIENT AUTHORIZATION --> Client ID: {}", xPagopaExtchCxId);
        return gestoreRepositoryCall.getClientConfiguration(xPagopaExtchCxId)
                                    .onErrorResume(RestCallException.ResourceNotFoundException.class,
                                                   throwable -> Mono.error(new ClientNotFoundException(xPagopaExtchCxId)));
    }
}
