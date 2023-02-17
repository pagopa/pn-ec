package it.pagopa.pn.ec.commons.rest.error;

import it.pagopa.pn.ec.commons.exception.ClientForbiddenException;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedFoundException;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException.IdClientNotFoundException;
import it.pagopa.pn.ec.commons.exception.RequestAlreadyInProgressException;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.exception.ss.AttachmentNotAvailableException;
import it.pagopa.pn.ec.rest.v1.dto.Problem;
import it.pagopa.pn.ec.rest.v1.dto.ProblemError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
@Slf4j
public class GlobalRestErrorHandler {

	private static final String DEFAULT_PROBLEM_ERROR_MESSAGE = "Internal error codes to be defined";

	@ExceptionHandler(ClientNotAuthorizedFoundException.class)
	public final ResponseEntity<Problem> handleUnauthorizedIdClient(ClientNotAuthorizedFoundException exception) {
		var problem = new Problem();
		problem.setStatus(UNAUTHORIZED.value());
		problem.setTitle("Client id not authorized");
		problem.setDetail(exception.getMessage());
		problem.setTraceId(UUID.randomUUID().toString());
		return new ResponseEntity<>(problem, UNAUTHORIZED);
	}
	
	@ExceptionHandler(ClientForbiddenException.class)
	public final ResponseEntity<Problem> handleForbiddenClient(ClientForbiddenException exception) {
		var problem = new Problem();
		problem.setStatus(FORBIDDEN.value());
		problem.setTitle("Client id not found");
		problem.setDetail(exception.getMessage());
		problem.setTraceId(UUID.randomUUID().toString());
		return new ResponseEntity<>(problem, FORBIDDEN);
	}
	
	@ExceptionHandler({ WebExchangeBindException.class, ConstraintViolationException.class })
	public final ResponseEntity<Problem> handleBadRequest(Exception exception) {
		var problem = new Problem();
		problem.setStatus(BAD_REQUEST.value());
		problem.setTitle("Bad request");
		problem.setDetail("Check the errors array");
		problem.setTraceId(UUID.randomUUID().toString());
		if (exception instanceof WebExchangeBindException webExchangeBindException) {
			problem.setErrors(webExchangeBindException.getFieldErrors().stream().map(fieldError -> {
				var problemError = new ProblemError();
				problemError.setCode(DEFAULT_PROBLEM_ERROR_MESSAGE);
				problemError.setElement(fieldError.getField());
				problemError.setDetail(fieldError.getDefaultMessage());
				return problemError;
			}).toList());
		} else if (exception instanceof ConstraintViolationException constraintViolationException) {
			problem.setErrors(
					constraintViolationException.getConstraintViolations().stream().map(constraintViolation -> {
						var problemError = new ProblemError();
						problemError.setCode(DEFAULT_PROBLEM_ERROR_MESSAGE);
						String field = null;
						for (Path.Node node : constraintViolation.getPropertyPath()) {
							field = node.getName();
						}
						problemError.setElement(field);
						problemError.setDetail(constraintViolation.getMessage());
						return problemError;
					}).toList());
		}
		return new ResponseEntity<>(problem, BAD_REQUEST);
	}

	@ExceptionHandler({ EcInternalEndpointHttpException.class, SqsPublishException.class, SnsSendException.class })
	public final ResponseEntity<Problem> handleAnotherServiceError(Exception exception) {
		var problem = new Problem();
		problem.setStatus(SERVICE_UNAVAILABLE.value());
		problem.setTitle("System outage");
		problem.setDetail(exception.getMessage());
		problem.setTraceId(UUID.randomUUID().toString());
		return new ResponseEntity<>(problem, SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(RequestAlreadyInProgressException.class)
	public final ResponseEntity<Problem> handleRequestAlreadyInProgress(RequestAlreadyInProgressException exception) {
		var problem = new Problem();
		problem.setStatus(CONFLICT.value());
		problem.setTitle("Request already in progress");
		problem.setDetail(exception.getMessage());
		problem.setTraceId(UUID.randomUUID().toString());
		return new ResponseEntity<>(problem, CONFLICT);
	}

	@ExceptionHandler(AttachmentNotAvailableException.class)
	public final ResponseEntity<Problem> handleAttachmentNotAvailable(AttachmentNotAvailableException exception) {
		var problem = new Problem();
		problem.setStatus(NOT_FOUND.value());
		problem.setTitle("Attachment not found");
		problem.setDetail(exception.getMessage());
		problem.setTraceId(UUID.randomUUID().toString());
		return new ResponseEntity<>(problem, NOT_FOUND);
	}
}
