package it.pagopa.pn.ec.service;

import it.pagopa.pn.ec.exception.IdClientNotFoundException;

public interface AuthService {

    /**
     * Metodo per verificare se il client id fornito in request è presente nell'anagrafica client
     * tramite il Gestore Repository
     * @param idClient
     * Client id da autenticare
     * @throws IdClientNotFoundException
     * Eccezione se l'id client non è stato trovato
     */
    void checkIdClient(String idClient) throws IdClientNotFoundException;
}
