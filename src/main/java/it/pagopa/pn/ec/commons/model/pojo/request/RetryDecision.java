package it.pagopa.pn.ec.commons.model.pojo.request;

import it.pagopa.pn.ec.rest.v1.dto.RequestDto;

public record RetryDecision(RequestDto requestDto, boolean exhausted) {
}
