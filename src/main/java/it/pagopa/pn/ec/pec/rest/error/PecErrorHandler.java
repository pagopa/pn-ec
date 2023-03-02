package it.pagopa.pn.ec.pec.rest.error;

import it.pagopa.pn.ec.pec.exception.MessageIdException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ControllerAdvice
public class PecErrorHandler {

    @ExceptionHandler(MessageIdException.DecodeMessageIdException.class)
    public final ResponseEntity<Problem> handleDecodeMessageIdException(MessageIdException.DecodeMessageIdException decodeMessageIdException) {
        var problem = new Problem();
        problem.setStatus(BAD_REQUEST.value());
        problem.setTitle("Malformed messageId");
        problem.setDetail(decodeMessageIdException.getMessage());
        problem.setTraceId(UUID.randomUUID().toString());
        return new ResponseEntity<>(problem, BAD_REQUEST);
    }
}
