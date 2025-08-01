package it.pagopa.pn.ec.commons.exception.cartaceo;

import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import lombok.EqualsAndHashCode;

import static it.pagopa.pn.ec.commons.utils.LogUtils.INVALID_TRANSFORMATION;

@EqualsAndHashCode(callSuper = true)
public class InvalidTransformationTypeException extends Generic400ErrorException {

    public InvalidTransformationTypeException(String details) {
        super(INVALID_TRANSFORMATION, details);
    }
}
