package it.pagopa.pn.ec.commons.rest.call.gestorerepository.richieste;

import it.pagopa.pn.ec.commons.constant.Status;
import reactor.core.publisher.Mono;

public interface RichiesteCall {

    // TODO: Aggiungere tutte le chiamate verso il gestore repository

    // TODO: Cambiare i tipi di ritorno delle chiamate una volta che saranno disponibili gli endpoint del gestore repository

    Mono<Status> getRichiesta(String idRequest);
}
