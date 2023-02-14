package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import reactor.core.publisher.Mono;

public interface CallMachinaStati {

    Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(String processId, String currStatus, String clientId, String nextStatus);
}
