package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.ClientNotFoundException;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationInternalDto;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Metodo per verificare se il client fornito in request è presente nell'anagrafica client
     * tramite il Gestore Repository
     *
     * @param idClient Client id da autenticare
     * @return Un  {@link Mono}<{@link ClientConfigurationDto}> associato all'id client trovato o un error signal di tipo
     * {@link ClientNotFoundException} se l'id
     * client non è stato trovato
     */
    Mono<ClientConfigurationInternalDto> clientAuth(final String idClient);

    Mono<ClientConfigurationInternalDto> validateApiKey(final String idClient, final String xApiKey);
}
