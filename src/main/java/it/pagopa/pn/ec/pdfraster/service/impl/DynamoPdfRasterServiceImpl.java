package it.pagopa.pn.ec.pdfraster.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.pdfraster.model.entity.AttachmentToConvert;
import it.pagopa.pn.ec.pdfraster.model.entity.PdfConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.RequestConversionEntity;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import lombok.CustomLog;
import org.springframework.stereotype.Service;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;
import static it.pagopa.pn.ec.commons.utils.LogUtils.*;


@Service
@CustomLog
public class DynamoPdfRasterServiceImpl implements DynamoPdfRasterService {


    private final DynamoDbAsyncTableDecorator<RequestConversionEntity> requestTable;
    private final DynamoDbAsyncTableDecorator<PdfConversionEntity> conversionTable;
    private final ObjectMapper objectMapper;


    public DynamoPdfRasterServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.requestTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversioneRequestName(),
                TableSchema.fromBean(RequestConversionEntity.class)));
        this.conversionTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversionePdfName(),
                TableSchema.fromBean(PdfConversionEntity.class)));
    }


    @Override
    public Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto requestConversionDto) {
        log.logStartingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION);
        return Mono.fromCallable(() -> convertToEntity(requestConversionDto))
                .flatMap(this::saveRequestConversionEntity)
                .flatMap(this::savePdfConversions)
                .map(this::convertToDto)
                .doOnSuccess(result -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION))
                .doOnError(throwable -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION, false, throwable.getMessage()));
    }

    private RequestConversionEntity convertToEntity(RequestConversionDto dto) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_CONVERT_TO_ENTITY, dto);
        dto.getAttachments().stream().filter(attachment ->  attachment.getConverted() == null ).forEach(attachment -> attachment.setConverted(false));
        return objectMapper.convertValue(dto, RequestConversionEntity.class);
    }

    private Mono<RequestConversionEntity> saveRequestConversionEntity(RequestConversionEntity entity) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_SAVE_REQUEST_CONVERSION_ENTITY, entity);
        return Mono.fromCallable( () ->{
                    requestTable.putItem(PutItemEnhancedRequest.builder(RequestConversionEntity.class)
                            .item(entity)
                            .build());
                    return entity; }
                )
                .doOnSuccess(result -> log.info(PDF_RASTER_SAVE_REQUEST_CONVERSION_ENTITY));

    }

    private Mono<RequestConversionEntity> savePdfConversions(RequestConversionEntity entity) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_SAVE_PDF_CONVERSIONS, entity);
        String requestId = entity.getRequestId();
        return Flux.fromIterable(entity.getAttachments())
                .map(attachment -> createPdfConversion(attachment, requestId))
                .flatMap(this::savePdfConversion)
                .then(Mono.just(entity))
                .doOnSuccess(result -> log.info(PDF_RASTER_SAVE_PDF_CONVERSION));

    }

    private PdfConversionEntity createPdfConversion(AttachmentToConvert attachment, String requestId) {
        log.debug(LOGGING_OPERATION_WITH_ARGS, PDF_RASTER_SAVE_PDF_CONVERSION, attachment, requestId);
        PdfConversionEntity pdfConversion = new PdfConversionEntity();
        pdfConversion.setFileKey(attachment.getNewFileKey().replace("safestorage://", ""));
        pdfConversion.setRequestId(requestId);
        pdfConversion.setExpiration(null);
        return pdfConversion;
    }

    private Mono<Void> savePdfConversion(PdfConversionEntity pdfConversion) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_SAVE_PDF_CONVERSIONS, pdfConversion);

        return Mono.fromCompletionStage(() ->
                    conversionTable.putItem(PutItemEnhancedRequest.builder(PdfConversionEntity.class)
                            .item(pdfConversion)
                            .build())
                )
                .then()
                .doOnSuccess(aVoid -> log.info(PDF_RASTER_SAVE_PDF_CONVERSION));
    }



    private RequestConversionDto convertToDto(RequestConversionEntity entity) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_CONVERT_TO_DTO, entity);
        return objectMapper.convertValue(entity, RequestConversionDto.class);
    }


    @Override
    public Mono<Map.Entry<RequestConversionDto, Boolean>> updateRequestConversion(String fileKey, Boolean converted, String fileHash) {
        log.logStartingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION);

        if (converted == null || !converted) {
            return Mono.error(new IllegalArgumentException("Invalid value for 'converted': must be true."));
        }

        return getPdfConversionFromDynamoDb(fileKey)
                .zipWhen(pdfConversionEntity ->
                        getRequestConversionFromDynamoDb(pdfConversionEntity.getRequestId())
                                .flatMap(requestConversionEntity ->
                                        updateAttachmentConversion(requestConversionEntity, fileKey, converted, fileHash)
                                                .map(entry -> Map.entry(convertToDto(entry.getKey()),entry.getValue()))
                                )
                )
                .map(tuples -> {
                    PdfConversionEntity pdfConversionEntity = tuples.getT1();
                    Map.Entry<RequestConversionDto, Boolean> requestConversion = tuples.getT2();
                    pdfConversionEntity.setExpiration(BigDecimal.valueOf(OffsetDateTime.now().plusDays(1).toInstant().getEpochSecond()));
                    conversionTable.putItem(pdfConversionEntity);
                    return requestConversion;
                })
                .doOnSuccess(result -> log.info(PDF_RASTER_UPDATE_REQUEST_CONVERSION))
                .doOnError(exception -> log.logEndingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION, false, exception.getMessage()))
                .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY);
    }

    private Mono<Map.Entry<RequestConversionEntity,Boolean>> updateAttachmentConversion(RequestConversionEntity requestConversionEntity, String fileKey, Boolean converted, String fileHash) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_UPDATE_ATTACHMENT_CONVERSION, requestConversionEntity);
        if(requestConversionEntity.getAttachments().parallelStream().allMatch(AttachmentToConvert::getConverted))
            return Mono.just(Map.entry(requestConversionEntity, false));
        requestConversionEntity.getAttachments().stream()
                .filter(attachment -> attachment.getNewFileKey().equals(fileKey))
                .findFirst()
                .ifPresent(attachment -> {
                    attachment.setConverted(converted);
                    attachment.setSha256(fileHash);
                });
        return Mono.fromFuture(requestTable.putItem(requestConversionEntity))
                .thenReturn(Map.entry(requestConversionEntity,true))
                .doOnSuccess(result -> log.info(PDF_RASTER_UPDATE_ATTACHMENT_CONVERSION));
    }

    private Mono<RequestConversionEntity> getRequestConversionFromDynamoDb(String requestId) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_GET_REQUEST_CONVERSION_FROM_DYNAMO_DB, requestId);
        return Mono.fromCompletionStage(() -> requestTable.getItem(Key.builder().partitionValue(requestId).build()))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestConversionNotFoundException(requestId)))
                .doOnSuccess(result -> log.info(PDF_RASTER_GET_REQUEST_CONVERSION_FROM_DYNAMO_DB));

    }

    private Mono<PdfConversionEntity> getPdfConversionFromDynamoDb(String fileKey) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_GET_PDF_CONVERSION_FROM_DYNAMO_DB, fileKey);
        return Mono.fromCompletionStage(() -> conversionTable.getItem(Key.builder().partitionValue(fileKey).build()))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.PdfConversionNotFoundException(fileKey)))
                .doOnSuccess(result -> log.info(PDF_RASTER_GET_PDF_CONVERSION_FROM_DYNAMO_DB));

    }


}
