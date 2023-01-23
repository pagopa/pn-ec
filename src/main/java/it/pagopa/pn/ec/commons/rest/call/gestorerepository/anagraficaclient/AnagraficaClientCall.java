package it.pagopa.pn.ec.commons.rest.call.gestorerepository.anagraficaclient;

import reactor.core.publisher.Mono;

public interface AnagraficaClientCall {

    // TODO: Aggiungere tutte le chiamate verso l'anagrafica client

    // TODO: Cambiare i tipi di ritorno delle chiamate una volta che saranno disponibili gli endpoint dell'anagrafica client

    Mono<String> getClient(String idClient);
}
