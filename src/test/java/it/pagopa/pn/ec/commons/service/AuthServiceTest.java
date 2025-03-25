package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.InvalidApiKeyException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.impl.AuthServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
class AuthServiceTest {
    public static final String CLIENT_ID = "clientId";
    public static final String VALID_API_KEY = "validApiKey";
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private AuthServiceImpl authService;

    @Test
    void clientAuth_Success() {
        ClientConfigurationInternalDto clientConfig = new ClientConfigurationInternalDto();
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfig));

        Mono<ClientConfigurationInternalDto> result = authService.clientAuth(CLIENT_ID);

        StepVerifier.create(result)
                .expectNext(clientConfig)
                .verifyComplete();
    }

    @Test
    void clientAuth_ClientNotFound() {
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.error(new ClientNotFoundException(CLIENT_ID)));

        Mono<ClientConfigurationInternalDto> result = authService.clientAuth(CLIENT_ID);

        StepVerifier.create(result)
                .expectError(ClientNotFoundException.class)
                .verify();
    }

    @Test
    void validateApiKey_Success() {
        ClientConfigurationInternalDto clientConfig = new ClientConfigurationInternalDto();
        clientConfig.setApiKey(VALID_API_KEY);
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfig));

        Mono<ClientConfigurationInternalDto> result = authService.validateApiKey(CLIENT_ID, VALID_API_KEY);

        StepVerifier.create(result)
                .expectNext(clientConfig)
                .verifyComplete();
    }

    @Test
    void validateApiKey_InvalidApiKey() {
        ClientConfigurationInternalDto clientConfig = new ClientConfigurationInternalDto();
        clientConfig.setApiKey(VALID_API_KEY);
        when(gestoreRepositoryCall.getClientConfiguration(anyString())).thenReturn(Mono.just(clientConfig));

        Mono<ClientConfigurationInternalDto> result = authService.validateApiKey(CLIENT_ID, "invalidApiKey");

        StepVerifier.create(result)
                .expectError(InvalidApiKeyException.class)
                .verify();
    }

}
