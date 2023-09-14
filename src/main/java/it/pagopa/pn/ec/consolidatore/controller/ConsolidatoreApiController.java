package it.pagopa.pn.ec.consolidatore.controller;

import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.*;
import static it.pagopa.pn.ec.consolidatore.utils.LogUtils.INVALID_API_KEY;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_MESSAGE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.COMPLETED_OK_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.INTERNAL_SERVER_ERROR_CODE;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.errorCodeDescriptionMap;
import java.util.ArrayList;
import java.util.List;
import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ss.SafeStorageEndpointProperties;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.consolidatore.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.consolidatore.exception.SyntaxException;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import it.pagopa.pn.ec.consolidatore.service.impl.ConsolidatoreServiceImpl;
import it.pagopa.pn.ec.consolidatore.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.rest.v1.api.ConsolidatoreApi;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadRequestData;
import it.pagopa.pn.ec.rest.v1.dto.PreLoadResponseData;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ConsolidatoreApiController implements ConsolidatoreApi {

    private static final Integer NRO_MAX_ERRORS = 50;
    private static final String LOG_LABEL = "ConsolidatoreApiController.sendPaperProgressStatusRequest() : ";
    private final ConsolidatoreServiceImpl consolidatoreServiceImpl;
    private final RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService;

    private final SafeStorageEndpointProperties safeStorageEndpointProperties;
    @Autowired
    private AuthService authService;

    public ConsolidatoreApiController(ConsolidatoreServiceImpl consolidatoreServiceImpl
            , RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService,
                                      SafeStorageEndpointProperties safeStorageEndpointProperties) {
        this.consolidatoreServiceImpl = consolidatoreServiceImpl;
        this.ricezioneEsitiCartaceoService = ricezioneEsitiCartaceoService;
        this.safeStorageEndpointProperties = safeStorageEndpointProperties;
    }

    private List<String> getAllErrors(List<OperationResultCodeResponse> responses) {
        var errors = new ArrayList<String>();
        if (responses == null) {
            return errors;
        }
        responses.forEach(response -> {
            if (!response.getResultCode().equals(COMPLETED_OK_CODE)) {
                errors.addAll(response.getErrorList());
            }
        });
        return errors;
    }

    private OperationResultCodeResponse getOperationResultCodeResponse(String resultCode, String resultDescription, List<String> errors) {

        if (errors != null && errors.size() > NRO_MAX_ERRORS) {
            errors = errors.subList(0, NRO_MAX_ERRORS - 1);
        }

        OperationResultCodeResponse response = new OperationResultCodeResponse();
        response.setResultCode(resultCode);
        response.setResultDescription(resultDescription);
        response.setErrorList(errors);
        return response;
    }

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaExtchServiceId, String xApiKey, final ServerWebExchange exchange) {
        return consolidatoreServiceImpl.getFile(fileKey, xPagopaExtchServiceId, xApiKey)
                .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute("requestBody")))
                .doOnError(SemanticException.class, e -> log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(e.getAuditLogErrorList())))
                .doOnError(SyntaxException.class, e -> log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(e.getAuditLogErrorList())))
                .map(ResponseEntity::ok);
    }


    @Override
    public Mono<ResponseEntity<PreLoadResponseData>> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, Mono<PreLoadRequestData> preLoadRequestData, ServerWebExchange exchange) {

        return consolidatoreServiceImpl.presignedUploadRequest(xPagopaExtchServiceId, xApiKey, preLoadRequestData)
                .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute("requestBody")))
                .doOnError(SemanticException.class, e -> log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(e.getAuditLogErrorList())))
                .doOnError(SyntaxException.class, e -> log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(e.getAuditLogErrorList())))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<OperationResultCodeResponse>> sendPaperProgressStatusRequest(String xPagopaExtchServiceId,
                                                                                            String xApiKey,
                                                                                            Flux<ConsolidatoreIngressPaperProgressStatusEvent> consolidatoreIngressPaperProgressStatusEvent,
                                                                                            final ServerWebExchange exchange) {
        log.info("START sendPaperProgressStatusRequest, clientID: {}", xPagopaExtchServiceId);
        return authService.clientAuth(xPagopaExtchServiceId)
                .flatMap(clientConfiguration -> {

                    if (clientConfiguration.getApiKey() == null || !clientConfiguration.getApiKey().equals(xApiKey)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_API_KEY));
                    }
                    return Mono.just(clientConfiguration);
                })
                .flatMap(clientConfiguration -> consolidatoreIngressPaperProgressStatusEvent
                        .flatMap(statusEvent -> {
                            log.debug(LOG_LABEL + "START for requestId {}", statusEvent.getRequestId());
                            return ricezioneEsitiCartaceoService.verificaEsitoDaConsolidatore(xPagopaExtchServiceId, statusEvent);
                        })
                        .collectList()
                        .flatMap(listRicezioneEsitiDto -> {
                            // ricerco errori
                            var listErrorResponse = listRicezioneEsitiDto.stream()
                                    .filter(ricezioneEsito -> ricezioneEsito.getOperationResultCodeResponse() != null &&
                                            ricezioneEsito.getOperationResultCodeResponse().getResultCode() != null &&
                                            !ricezioneEsito.getOperationResultCodeResponse().getResultCode().equals(COMPLETED_OK_CODE))
                                    .toList();

                            if (listErrorResponse.isEmpty()) {
                                log.debug(LOG_LABEL + "Non ci sono errori sintattici/semantici");

                                // eventi
                                var listEvents = new ArrayList<ConsolidatoreIngressPaperProgressStatusEvent>();
                                listRicezioneEsitiDto.forEach(dto -> {
                                    if (dto.getPaperProgressStatusEvent() != null) {
                                        listEvents.add(dto.getPaperProgressStatusEvent());
                                    }
                                });

                                return publishOnQueue(listEvents, xPagopaExtchServiceId);

                            } else {
                                log.debug(LOG_LABEL + "errori sintattici/semantici : Sono stati individuati {} macro errori", listErrorResponse.size());
                                // errori
                                var listErrors = new ArrayList<OperationResultCodeResponse>();
                                var consAuditLogErrorList = new ArrayList<ConsAuditLogError>();

                                listErrorResponse.forEach(dto -> {
                                    if (dto.getConsAuditLogErrorList() != null) {
                                        consAuditLogErrorList.addAll(dto.getConsAuditLogErrorList());
                                    }

                                    if (dto.getOperationResultCodeResponse() != null) {
                                        listErrors.add(dto.getOperationResultCodeResponse());
                                    }
                                });

                                log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(consAuditLogErrorList));

                                var errors = getAllErrors(listErrors);
                                log.debug(LOG_LABEL + "errori sintattici/semantici : "
                                                + "result code = \"{}\" : "
                                                + "result description = \"{}\" : "
                                                + "specifici errori individuati = {}",
                                        listErrors.get(0).getResultCode(),
                                        listErrors.get(0).getResultDescription(),
                                        errors);
                                return Mono.just(ResponseEntity
                                        .badRequest()
                                        .body(getOperationResultCodeResponse(listErrors.get(0).getResultCode(),
                                                listErrors.get(0).getResultDescription(),
                                                errors)));
                            }
                        })
                        .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute("requestBody"))))
                        .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
                            log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute("requestBody")).errorList(throwable.getAuditLogErrorList()));
                            return Mono.just(ResponseEntity.internalServerError()
                                  .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
                                    errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
                                    List.of(throwable.getMessage()))));
                         })
                        .onErrorResume(RuntimeException.class, throwable -> {
                            log.error(LOG_LABEL + "* FATAL * errore generico = {}, {}", throwable, throwable.getMessage());
                            return Mono.just(ResponseEntity.internalServerError()
                                    .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
                                            errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
                                            List.of(throwable.getMessage()))));
                        });
    }


    private Mono<ResponseEntity<OperationResultCodeResponse>> publishOnQueue(List<ConsolidatoreIngressPaperProgressStatusEvent> listEvents, String xPagopaExtchServiceId){
        return Flux.fromIterable(listEvents)
                // pubblicazione sulla coda
                .flatMap(statusEvent -> ricezioneEsitiCartaceoService.pubblicaEsitoCodaNotificationTracker(xPagopaExtchServiceId, statusEvent))
                .collectList()
                // gestione errori oppure response ok
                .flatMap(listSendResponse -> {
                    var listSendErrorResponse = listSendResponse.stream().filter(response -> response.getResultCode() != null && !response.getResultCode().equals(COMPLETED_OK_CODE)).toList();
                    if (listSendErrorResponse.isEmpty()) {
                        log.debug(LOG_LABEL + "OK END");
                        return Mono.just(ResponseEntity.ok()
                                .body(getOperationResultCodeResponse(COMPLETED_OK_CODE,
                                        COMPLETED_MESSAGE,
                                        null)));
                    } else {
                        var sendErrors = getAllErrors(listSendErrorResponse);
                        log.debug(LOG_LABEL + "pubblicazione coda : errori individuati = {}", sendErrors);
                        return Mono.just(ResponseEntity.internalServerError()
                                .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
                                        errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
                                        sendErrors)));
                    }
                })
                .doOnError(RuntimeException.class, throwable ->
                        log.warn("* FATAL * publishOnQueue - {}, {}", throwable, throwable.getMessage()));
    }

    private void fieldValidationAuditLog(List<FieldError> errors, Object request) {
        List<ConsAuditLogError> consAuditLogErrorList = new ArrayList<>();
        for (FieldError error : errors) {
            String description = String.format("%s - %s", error.getField(), error.getDefaultMessage());
            var consAuditLogError = new ConsAuditLogError().description(description).error(ERR_CONS_BAD_JSON_FORMAT.getValue());
            consAuditLogErrorList.add(consAuditLogError);
        }
        log.error("{} - {}", ERR_CONS, new ConsAuditLogEvent<>().request(request).errorList(consAuditLogErrorList));
    }

}
