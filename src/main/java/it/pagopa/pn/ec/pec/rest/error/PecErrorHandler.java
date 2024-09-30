package it.pagopa.pn.ec.pec.rest.error;

import it.pagopa.pn.ec.commons.exception.InvalidReceiverDigitalAddressException;
import it.pagopa.pn.ec.pec.exception.MessageIdException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class PecErrorHandler {

    @ExceptionHandler(MessageIdException.EncodeMessageIdException.class)
    public final ResponseEntity<Problem> handleEncodeMessageIdException(MessageIdException.EncodeMessageIdException encodeMessageIdException) {
        var problem = new Problem();
        problem.setStatus(INTERNAL_SERVER_ERROR.value());
        problem.setTitle("An error occurred during messageId encoding");
        problem.setDetail(encodeMessageIdException.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MessageIdException.DecodeMessageIdException.class)
    public final ResponseEntity<Problem> handleDecodeMessageIdException(MessageIdException.DecodeMessageIdException decodeMessageIdException) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Malformed messageId");
        problem.setDetail(decodeMessageIdException.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }

    @ExceptionHandler(InvalidReceiverDigitalAddressException.class)
    public final ResponseEntity<Problem> handleInvalidReceiverDigitalAddressException(InvalidReceiverDigitalAddressException invalidReceiverDigitalAddressException) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Invalid receiver digital address");
        problem.setDetail(invalidReceiverDigitalAddressException.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
