package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public abstract class PresaInCaricoService {

    private final AuthService authService;

    protected PresaInCaricoService(AuthService authService) {
        this.authService = authService;
    }

    public Mono<Void> presaInCarico(PresaInCaricoInfo presaInCaricoInfo) throws RequestAlreadyInProgressException {
        return authService.clientAuth(presaInCaricoInfo.getXPagopaExtchCxId()).then(specificPresaInCarico(presaInCaricoInfo));
    }

    protected abstract Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo);
}
