package it.pagopa.pn.ec.commons.rest.call.machinestate;

import it.pagopa.pn.ec.commons.exception.InvalidNextStatusException;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiValidateStatoResponseDto;
import reactor.core.publisher.Mono;

public interface CallMacchinaStati {

    class StatusValidationBadRequestException extends RuntimeException {

        public StatusValidationBadRequestException() {
            super("Check if all paths and query params are present");
        }
    }

    Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(String xPagopaExtchCxId, String processId,
                                                                 String currentStatus, String nextStatus)
            throws InvalidNextStatusException;

    Mono<MacchinaStatiDecodeResponseDto> statusDecode(String xPagopaExtchCxId, String processId, String statusToDecode);
}
