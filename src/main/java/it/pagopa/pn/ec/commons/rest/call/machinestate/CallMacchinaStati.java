package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pn.ec.commons.model.pojo.request.RequestStatusChange;
import reactor.core.publisher.Mono;

public interface CallMacchinaStati {

    Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(RequestStatusChange requestStatusChange) throws InvalidNextStatusException;

    Mono<MacchinaStatiDecodeResponseDto> statusDecode(RequestStatusChange requestStatusChange);
}
