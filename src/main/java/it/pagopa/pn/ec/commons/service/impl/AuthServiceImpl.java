package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.IdClientNotFoundException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.anagraficaclient.AnagraficaClientCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AnagraficaClientCall anagraficaClientCall;

    public AuthServiceImpl(AnagraficaClientCall anagraficaClientCall) {
        this.anagraficaClientCall = anagraficaClientCall;
    }

    @Override
    public Mono<Void> clientAuth(final String idClient) throws IdClientNotFoundException {
        log.info("<-- Start client authentication -->");
        log.info("Id client -> {}", idClient);
        return anagraficaClientCall.getClient(idClient)
                                   .switchIfEmpty(Mono.error(new IdClientNotFoundException(idClient)))
                                   .then();

    }
}
