package it.pagopa.pn.ec.richiestemetadati.controller;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.rest.v1.api.PaperRequestMetadataPatchApi;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataPatchRequest;
import it.pagopa.pn.ec.richiestemetadati.service.impl.PaperRequestMetadataPatchServiceImpl;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@RestController
@CustomLog
public class PaperRequestMetadataPatchController implements PaperRequestMetadataPatchApi {

    private final PaperRequestMetadataPatchServiceImpl richiesteMetadatireworkService;

    public PaperRequestMetadataPatchController(PaperRequestMetadataPatchServiceImpl richiesteMetadatireworkService) {

        this.richiesteMetadatireworkService = richiesteMetadatireworkService;
    }
    @Override
    public  Mono<ResponseEntity<Void>> patchRequestMetadata(String requestIdx, String xPagopaExtchCxId, Mono<RequestMetadataPatchRequest> requestMetadataPatchRequest,  final ServerWebExchange exchange) {
        log.logStartingProcess(PAPER_REQUEST_METADATA_REWORK);
        return MDCUtils.addMDCToContextAndExecute(
                requestMetadataPatchRequest.flatMap(req ->
                richiesteMetadatireworkService.patchIsOpenReworkRequest(xPagopaExtchCxId,requestIdx,req)
                        .doOnSuccess(result -> log.logEndingProcess(PAPER_REQUEST_METADATA_REWORK))
                        .doOnError(throwable -> log.logEndingProcess(PAPER_REQUEST_METADATA_REWORK, false, throwable.getMessage()))
                        .thenReturn(ResponseEntity.noContent().<Void>build())
                        .onErrorResume(this::handleError)
                ));
    }

    private Mono<ResponseEntity<Void>> handleError(Throwable throwable) {
        log.error("Error processing patch request: {}", throwable.getMessage(), throwable);

        if (throwable instanceof RepositoryManagerException.RequestNotFoundException) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    throwable.getMessage(),
                    throwable
            ));
        }

        if (throwable instanceof RepositoryManagerException.RequestMalformedException) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    throwable.getMessage(),
                    throwable
            ));
        }

        return Mono.error(new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                throwable.getMessage(),
                throwable
        ));
    }
}
