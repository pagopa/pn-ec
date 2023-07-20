package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.InvalidApiKeyException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException.IdClientNotFoundException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogEvent;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.ERR_CONS;
import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.ERR_CONS_BAD_API_KEY;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final GestoreRepositoryCall gestoreRepositoryCall;

    public AuthServiceImpl(GestoreRepositoryCall gestoreRepositoryCall) {
        this.gestoreRepositoryCall = gestoreRepositoryCall;
    }

    @Override
    public Mono<ClientConfigurationInternalDto> clientAuth(final String xPagopaExtchCxId) {
        log.info(CHECKING_VALIDATION_PROCESS_ON, CLIENT_AUTHENTICATION, xPagopaExtchCxId);
        return gestoreRepositoryCall.getClientConfiguration(xPagopaExtchCxId)
                .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> Mono.error(new ClientNotFoundException(xPagopaExtchCxId)))
                .doOnNext(result->log.info(VALIDATION_PROCESS_PASSED, CLIENT_AUTHENTICATION))
                .doOnError(throwable -> log.warn(VALIDATION_PROCESS_FAILED, CLIENT_AUTHENTICATION, throwable.getMessage()));
    }

    @Override
    public Mono<ClientConfigurationInternalDto> validateApiKey(String idClient, String xApiKey) {
        return clientAuth(idClient).flatMap(clientConfiguration -> {
            log.info(CHECKING_VALIDATION_PROCESS_ON, X_API_KEY_VALIDATION, idClient);
            if (!clientConfiguration.getApiKey().equals(xApiKey)) {
                log.warn(VALIDATION_PROCESS_FAILED, X_API_KEY_VALIDATION, INVALID_API_KEY);
                return Mono.error(new InvalidApiKeyException());
            }
            log.info(VALIDATION_PROCESS_PASSED, X_API_KEY_VALIDATION);
            return Mono.just(clientConfiguration);
        });
    }
}
