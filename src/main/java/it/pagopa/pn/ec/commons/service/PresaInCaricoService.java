package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@Service
@Slf4j
public abstract class PresaInCaricoService {

    private final AuthService authService;

    protected PresaInCaricoService(AuthService authService) {
        this.authService = authService;
    }

    public Mono<Void> presaInCarico(PresaInCaricoInfo presaInCaricoInfo) {
        return authService.clientAuth(presaInCaricoInfo.getXPagopaExtchCxId())
                .flatMap(clientConfigurationDto -> specificPresaInCarico(presaInCaricoInfo));
    }

    protected abstract Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo);
}
