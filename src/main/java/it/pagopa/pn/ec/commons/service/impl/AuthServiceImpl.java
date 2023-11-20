package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.InvalidApiKeyException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class AuthServiceImpl implements AuthService {

    private final GestoreRepositoryCall gestoreRepositoryCall;

    public AuthServiceImpl(GestoreRepositoryCall gestoreRepositoryCall) {
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    @Override
    public Mono<ClientConfigurationInternalDto> clientAuth(final String xPagopaExtchCxId) {
        log.logChecking(CLIENT_AUTHENTICATION);
        return gestoreRepositoryCall.getClientConfiguration(xPagopaExtchCxId)
                .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> Mono.error(new ClientNotFoundException(xPagopaExtchCxId)))
                .doOnError(throwable -> log.logCheckingOutcome(CLIENT_AUTHENTICATION, false, throwable.getMessage()))
                .doOnNext(result->log.logCheckingOutcome(CLIENT_AUTHENTICATION, true));
    }

    @Override
    public Mono<ClientConfigurationInternalDto> validateApiKey(String idClient, String xApiKey) {
        return clientAuth(idClient).flatMap(clientConfiguration -> {
            log.logChecking(X_API_KEY_VALIDATION);
            if (!clientConfiguration.getApiKey().equals(xApiKey)) {
                log.logCheckingOutcome(X_API_KEY_VALIDATION, false, INVALID_API_KEY);
                return Mono.error(new InvalidApiKeyException());
            }
            log.logCheckingOutcome(X_API_KEY_VALIDATION, true);
            return Mono.just(clientConfiguration);
        });
    }
}
