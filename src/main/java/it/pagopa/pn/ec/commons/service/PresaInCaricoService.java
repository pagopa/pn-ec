package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.RetryDto;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@CustomLog
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

    protected static boolean isRetryExhausted(RetryDto retry) {
        return retry != null && retry.getRetryStep().compareTo(BigDecimal.valueOf(retry.getRetryPolicy().size())) >= 0;
    }
}
