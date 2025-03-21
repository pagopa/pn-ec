package it.pagopa.pn.ec.consolidatore.controller;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.exception.ShaGenerationException;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.consolidatore.exception.SemanticException;
import it.pagopa.pn.ec.consolidatore.exception.SyntaxException;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogEvent;
import it.pagopa.pn.ec.consolidatore.service.RicezioneEsitiCartaceoService;
import it.pagopa.pn.ec.consolidatore.service.impl.ConsolidatoreServiceImpl;
import it.pagopa.pn.ec.rest.v1.api.ConsolidatoreApi;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;
import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.ERR_CONS;
import static it.pagopa.pn.ec.consolidatore.constant.ConsAuditLogEventType.ERR_CONS_BAD_JSON_FORMAT;
import static it.pagopa.pn.ec.consolidatore.service.impl.RicezioneEsitiCartaceoServiceImpl.getAllErrors;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.*;

@RestController
@CustomLog
public class ConsolidatoreApiController implements ConsolidatoreApi {

    private static final Integer NRO_MAX_ERRORS = 50;
    public static final String REQUEST_BODY = "requestBody";
    public static final String LOG_FORMAT = "{} - {}";
    private final ConsolidatoreServiceImpl consolidatoreServiceImpl;
    private final RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService;
    private final AuthService authService;
    private static final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String PATTERN_DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter TIMESTAMP_RICEZIONE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));
    private static final DateTimeFormatter DATA_RICEZIONE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATE_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));

    public ConsolidatoreApiController(ConsolidatoreServiceImpl consolidatoreServiceImpl
            , RicezioneEsitiCartaceoService ricezioneEsitiCartaceoService, AuthService authService) {
        this.consolidatoreServiceImpl = consolidatoreServiceImpl;
        this.ricezioneEsitiCartaceoService = ricezioneEsitiCartaceoService;
        this.authService = authService;
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
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(GET_FILE);
        return MDCUtils.addMDCToContextAndExecute(consolidatoreServiceImpl.getFile(fileKey, xPagopaExtchServiceId, xApiKey)
                .doOnSuccess(result -> log.logEndingProcess(GET_FILE))
                .doOnError(throwable -> log.logEndingProcess(GET_FILE, false, throwable.getMessage()))
                .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute(REQUEST_BODY)))
                .doOnError(SemanticException.class, e -> log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute(REQUEST_BODY)).errorList(e.getAuditLogErrorList())))
                .doOnError(SyntaxException.class, e -> log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute(REQUEST_BODY)).errorList(e.getAuditLogErrorList())))
                .map(ResponseEntity::ok));
    }


    @Override
    public Mono<ResponseEntity<PreLoadResponseData>> presignedUploadRequest(String xPagopaExtchServiceId, String xApiKey, Mono<PreLoadRequestData> preLoadRequestData, ServerWebExchange exchange) {
        MDC.clear();
        log.logStartingProcess(PRESIGNED_UPLOAD_REQUEST_PROCESS);
        return consolidatoreServiceImpl.presignedUploadRequest(xPagopaExtchServiceId, xApiKey, preLoadRequestData)
                .doOnSuccess(result -> log.logEndingProcess(PRESIGNED_UPLOAD_REQUEST_PROCESS))
                .doOnError(throwable -> log.logEndingProcess(PRESIGNED_UPLOAD_REQUEST_PROCESS, false, throwable.getMessage()))
                .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute(REQUEST_BODY)))
                .doOnError(SemanticException.class, e -> log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute(REQUEST_BODY)).errorList(e.getAuditLogErrorList())))
                .doOnError(SyntaxException.class, e -> log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute(REQUEST_BODY)).errorList(e.getAuditLogErrorList())))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<OperationResultCodeResponse>> sendPaperProgressStatusRequest(String xPagopaExtchServiceId,
                                                                                            String xApiKey,
                                                                                            Flux<ConsolidatoreIngressPaperProgressStatusEvent> consolidatoreIngressPaperProgressStatusEvent,
                                                                                            final ServerWebExchange exchange) {
        MDC.clear();
        log.logStartingProcess(SEND_PAPER_PROGRESS_STATUS_REQUEST);
        OffsetDateTime now = OffsetDateTime.now();
        String timestampRicezione = now.format(TIMESTAMP_RICEZIONE_FORMATTER);
        String dataRicezione = now.format(DATA_RICEZIONE_FORMATTER);
        return authService.clientAuth(xPagopaExtchServiceId)
                .flatMap(clientConfiguration -> {
                    log.logChecking(X_API_KEY_VALIDATION);
                    if (clientConfiguration.getApiKey() == null || !clientConfiguration.getApiKey().equals(xApiKey)) {
                        log.logCheckingOutcome(X_API_KEY_VALIDATION, false, INVALID_API_KEY);
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_API_KEY));
                    }
                    log.logCheckingOutcome(X_API_KEY_VALIDATION, true);
                    return Mono.just(clientConfiguration);
                })
                .flatMap(clientConfiguration -> consolidatoreIngressPaperProgressStatusEvent
                        .flatMap(statusEvent -> {
                            MDC.put(MDC_CORR_ID_KEY, concatRequestId(xPagopaExtchServiceId, statusEvent.getRequestId()));
                            log.debug(SEND_PAPER_PROGRESS_STATUS_REQUEST + "START for requestId {}", statusEvent.getRequestId());
                            return MDCUtils.addMDCToContextAndExecute(ricezioneEsitiCartaceoService.verificaEsitoDaConsolidatore(xPagopaExtchServiceId, statusEvent));
                        })
                        .collectList()
                        .flatMap(listRicezioneEsitiDto -> {
                            MDC.clear();
                            // ricerco errori
                            var listErrorResponse = listRicezioneEsitiDto.stream()
                                    .filter(ricezioneEsito -> ricezioneEsito.getOperationResultCodeResponse() != null &&
                                            ricezioneEsito.getOperationResultCodeResponse().getResultCode() != null &&
                                            !ricezioneEsito.getOperationResultCodeResponse().getResultCode().equals(COMPLETED_OK_CODE))
                                    .toList();

                            if (listErrorResponse.isEmpty()) {

                                // eventi
                                var listEvents = new ArrayList<ConsolidatoreIngressPaperProgressStatusEvent>();
                                listRicezioneEsitiDto.forEach(dto -> {
                                    if (dto.getPaperProgressStatusEvent() != null) {
                                        listEvents.add(dto.getPaperProgressStatusEvent());
                                    }
                                });

                                return ricezioneEsitiCartaceoService.publishOnQueue(listEvents, xPagopaExtchServiceId);

                            } else {
                                log.debug(SEND_PAPER_PROGRESS_STATUS_REQUEST + ": syntax/semantic errors : {} macro errors have been detected", listErrorResponse.size());
                                // errori
                                var listErrors = new ArrayList<OperationResultCodeResponse>();
                                var consAuditLogErrorList = new ArrayList<ConsAuditLogError>();
                                var discardedEventsDtoList = new ArrayList<DiscardedEventDto>();

                                String jsonRequestBody = exchange.getAttribute(REQUEST_BODY);
                                String jsonRequestBodyHash = generateSha256(jsonRequestBody.getBytes(StandardCharsets.UTF_8));
                                listErrorResponse.forEach(dto -> {
                                    if (dto.getConsAuditLogErrorList() != null) {
                                        consAuditLogErrorList.addAll(dto.getConsAuditLogErrorList());
                                    }

                                    if (dto.getOperationResultCodeResponse() != null) {
                                        String requestId= dto.getPaperProgressStatusEvent().getRequestId();
                                        dto.getOperationResultCodeResponse().getErrorList().forEach(error -> {
                                            DiscardedEventDto discardedEventDto = new DiscardedEventDto();
                                            discardedEventDto.setDataRicezione(dataRicezione);
                                                discardedEventDto.setJsonRicevuto(jsonRequestBody);
                                                discardedEventDto.setPayloadHash(jsonRequestBodyHash);
                                            discardedEventDto.setCodiceScarto(error);
                                            discardedEventDto.setTimestampRicezione(timestampRicezione);
                                            discardedEventDto.setRequestId(requestId);
                                            discardedEventsDtoList.add(discardedEventDto);
                                        });
                                        listErrors.add(dto.getOperationResultCodeResponse());
                                    }
                                });

                                log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(exchange.getAttribute(REQUEST_BODY)).errorList(consAuditLogErrorList));

                                var errors = getAllErrors(listErrors);
                                log.debug(SEND_PAPER_PROGRESS_STATUS_REQUEST + "syntax/semantic errors : result code = '{}' : result description = '{}' : specific errors identified = {}",
                                        listErrors.get(0).getResultCode(),
                                        listErrors.get(0).getResultDescription(),
                                        errors);

                                var response = ResponseEntity
                                        .badRequest()
                                        .body(getOperationResultCodeResponse(listErrors.get(0).getResultCode(),
                                                listErrors.get(0).getResultDescription(),
                                                errors));

                                if (!discardedEventsDtoList.isEmpty()) {
                                    return ricezioneEsitiCartaceoService.insertDiscardedEvents(discardedEventsDtoList).then(Mono.just(response));
                                }
                                return Mono.just(response);
                            }
                        })
                        .doOnSuccess(result -> log.logEndingProcess(SEND_PAPER_PROGRESS_STATUS_REQUEST))
                        .doOnError(throwable -> log.logEndingProcess(SEND_PAPER_PROGRESS_STATUS_REQUEST, false, throwable.getMessage()))
                        .doOnError(WebExchangeBindException.class, e -> fieldValidationAuditLog(e.getFieldErrors(), exchange.getAttribute(REQUEST_BODY))))
                        .onErrorResume(RuntimeException.class, throwable -> {
                            String fatalMessage = throwable.getClass() == WebExchangeBindException.class ? "" : "* FATAL * ";
                            log.error(SEND_PAPER_PROGRESS_STATUS_REQUEST +  fatalMessage + "errore generico = {}, {}", throwable, throwable.getMessage());
                            return Mono.just(ResponseEntity.internalServerError()
                                    .body(getOperationResultCodeResponse(INTERNAL_SERVER_ERROR_CODE,
                                            errorCodeDescriptionMap().get(INTERNAL_SERVER_ERROR_CODE),
                                            List.of(throwable.getMessage()))));
                        });
    }

    private void fieldValidationAuditLog(List<FieldError> errors, Object request) {
        List<ConsAuditLogError> consAuditLogErrorList = new ArrayList<>();
        for (FieldError error : errors) {
            String description = String.format("%s - %s", error.getField(), error.getDefaultMessage());
            var consAuditLogError = new ConsAuditLogError().description(description).error(ERR_CONS_BAD_JSON_FORMAT.getValue());
            consAuditLogErrorList.add(consAuditLogError);
        }
        log.error(LOG_FORMAT, ERR_CONS, new ConsAuditLogEvent<>().request(request).errorList(consAuditLogErrorList));
    }

    private String generateSha256(byte[] fileBytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256");
            md.update(fileBytes);
            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            throw new ShaGenerationException(e.getMessage());
        }
    }

}
