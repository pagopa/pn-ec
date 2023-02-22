package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import reactor.core.publisher.Mono;

public interface AuthService {

    /**
     * Metodo per verificare se il client fornito in request è presente nell'anagrafica client
     * tramite il Gestore Repository
     * @param idClient
     * Client id da autenticare
     * @throws ClientNotAuthorizedException
     * Eccezione se l'id client non è stato trovato
     */
    Mono<Void> clientAuth(final String idClient) throws ClientNotAuthorizedException;
}
