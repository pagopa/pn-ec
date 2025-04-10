package it.pagopa.pn.ec.pdfraster.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.pdfraster.configuration.PdfRasterProperties;
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
import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;


@Service
@CustomLog
public class DynamoPdfRasterServiceImpl implements DynamoPdfRasterService {


    private final DynamoDbAsyncTableDecorator<RequestConversionEntity> requestTable;
    private final DynamoDbAsyncTableDecorator<PdfConversionEntity> conversionTable;
    private final TableSchema<PdfConversionEntity> pdfConversionTableSchema;
    private final TableSchema<RequestConversionEntity> requestConversionTableSchema;
    private final ObjectMapper objectMapper;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final Retry pdfRasterRetryStrategy;
    private static final String VERSION_CONDITIONAL_CHECK = "attribute_exists(version) AND version = :version";
    private final Integer offsetDays;


    public DynamoPdfRasterServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient,
                                      RepositoryManagerDynamoTableName repositoryManagerDynamoTableName, ObjectMapper objectMapper, DynamoDbAsyncClient dynamoDbAsyncClient, PdfRasterProperties pdfRasterProperties) {
        this.objectMapper = objectMapper;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.requestConversionTableSchema = TableSchema.fromBean(RequestConversionEntity.class);
        this.pdfConversionTableSchema = TableSchema.fromBean(PdfConversionEntity.class);
        this.requestTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversioneRequestName(), this.requestConversionTableSchema));
        this.conversionTable = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.richiesteConversionePdfName(), this.pdfConversionTableSchema));
        this.pdfRasterRetryStrategy = Retry.backoff(pdfRasterProperties.maxRetryAttempts(), Duration.ofSeconds(pdfRasterProperties.minRetryBackoff()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                .doBeforeRetry(retrySignal -> log.debug("Retry number {}, caused by : {}", retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()));
        this.offsetDays = pdfRasterProperties.pdfConversionExpirationOffsetInDays();
    }

    @Override
    public Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto requestConversionDto) {
        log.logStartingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION);
        return Mono.fromCallable(() -> convertToEntity(requestConversionDto))
                .flatMap(this::saveRequestConversionWithTransaction)
                .map(this::convertToDto)
                .doOnSuccess(result -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION))
                .doOnError(throwable -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION, false, throwable.getMessage()));
    }


    private RequestConversionEntity convertToEntity(RequestConversionDto dto) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_CONVERT_TO_ENTITY, dto);
        dto.getAttachments().stream().filter(attachment ->  attachment.getConverted() == null ).forEach(attachment -> attachment.setConverted(Boolean.FALSE));
        return objectMapper.convertValue(dto, RequestConversionEntity.class);
    }


    private PdfConversionEntity createPdfConversion(AttachmentToConvert attachment, String requestId) {
        log.debug(LOGGING_OPERATION_WITH_ARGS, PDF_RASTER_SAVE_PDF_CONVERSION, attachment, requestId);
        PdfConversionEntity pdfConversion = new PdfConversionEntity();
        pdfConversion.setFileKey(attachment.getNewFileKey().replace("safestorage://", ""));
        pdfConversion.setRequestId(requestId);
        pdfConversion.setExpiration(null);
        return pdfConversion;
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

        return processUpdateRequestConversion(fileKey, converted, fileHash)
                .doOnSuccess(result -> log.info(PDF_RASTER_UPDATE_REQUEST_CONVERSION))
                .doOnError(exception -> log.logEndingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION, false, exception.getMessage()))
                .retryWhen(pdfRasterRetryStrategy);
    }


    private Mono<Map.Entry<RequestConversionEntity, Boolean>> updateAttachmentInRequestConversion(RequestConversionEntity requestConversionEntity, String fileKey, Boolean converted, String fileHash) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, PDF_RASTER_UPDATE_ATTACHMENT_CONVERSION, requestConversionEntity);
        if (requestConversionEntity.getAttachments().parallelStream().anyMatch(attachmentToConvert -> attachmentToConvert.getNewFileKey().equals(fileKey) && attachmentToConvert.getConverted()))
            return Mono.just(Map.entry(requestConversionEntity, false));

        requestConversionEntity.getAttachments().stream()
                .filter(attachment -> attachment.getNewFileKey().equals(fileKey))
                .findFirst()
                .ifPresent(attachment -> {
                    attachment.setConverted(converted);
                    attachment.setSha256(fileHash);
                });

        return Mono.just(Map.entry(requestConversionEntity, true))
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


    private Mono<RequestConversionEntity> saveRequestConversionWithTransaction(RequestConversionEntity entity) {
        return Flux.fromIterable(entity.getAttachments())
                .map(attachment -> createPdfConversion(attachment, entity.getRequestId()))
                .map(pdfConversionEntity -> {
                    pdfConversionEntity.setVersion(1L);
                    Map<String, AttributeValue> pdfAttributes = pdfConversionTableSchema.itemToMap(pdfConversionEntity, true);
                    return TransactWriteItem.builder()
                            .put(put -> put
                                    .tableName(conversionTable.tableName())
                                    .item(pdfAttributes))
                            .build();
                })
                .collectList()
                .map(transactWriteItems -> {
                    entity.setVersion(1L);
                    Map<String, AttributeValue> itemAttributes = requestConversionTableSchema.itemToMap(entity, true);
                    TransactWriteItem transactWriteItemRequestConversion = TransactWriteItem.builder()
                            .put(put -> put
                                    .tableName(requestTable.tableName())
                                    .item(itemAttributes))
                            .build();
                    transactWriteItems.add(transactWriteItemRequestConversion);
                    return transactWriteItems;
                })
                .flatMap(this::executeTransactionWithAsyncClient)
                .thenReturn(entity);
    }


    private Mono<Void> updateRequestConversionWithTransaction(RequestConversionEntity requestConversionEntity, PdfConversionEntity pdfConversionEntity) {
        return Mono.just(pdfConversionEntity)
                .map(entity -> {
                    Long currVersion = pdfConversionEntity.getVersion();
                    entity.setVersion(currVersion + 1L);
                    Map<String, AttributeValue> pdfItemAttributes = pdfConversionTableSchema.itemToMap(entity, true);
                    return TransactWriteItem.builder()
                            .put(put -> put
                                    .tableName(conversionTable.tableName())
                                    .item(pdfItemAttributes)
                                    .expressionAttributeValues(Map.of(":version", AttributeValue.fromN(String.valueOf(currVersion))))
                                    .conditionExpression(VERSION_CONDITIONAL_CHECK))
                            .build();
                })
                .map(transactWriteItems -> {
                    Long currVersion = requestConversionEntity.getVersion();
                    requestConversionEntity.setVersion(currVersion + 1L);
                    List<TransactWriteItem> transactWriteItemsList = new ArrayList<>();
                    Map<String, AttributeValue> requestItemAttributes = requestConversionTableSchema.itemToMap(requestConversionEntity, true);
                    TransactWriteItem transactWriteItemRequestConversion = TransactWriteItem.builder()
                            .put(put -> put
                                    .tableName(requestTable.tableName())
                                    .item(requestItemAttributes)
                                    .expressionAttributeValues(Map.of(":version", AttributeValue.fromN(String.valueOf(currVersion))))
                                    .conditionExpression(VERSION_CONDITIONAL_CHECK))
                            .build();
                    transactWriteItemsList.add(transactWriteItemRequestConversion);
                    transactWriteItemsList.add(transactWriteItems);
                    return transactWriteItemsList;
                })
                .flatMap(this::executeTransactionWithAsyncClient);
    }


    private Mono<Map.Entry<RequestConversionDto, Boolean>> processUpdateRequestConversion(String fileKey, Boolean converted, String fileHash) {
        return getPdfConversionFromDynamoDb(fileKey)
                .zipWhen(pdfConversionEntity ->
                        getRequestConversionFromDynamoDb(pdfConversionEntity.getRequestId())
                                .flatMap(requestConversionEntity ->
                                        updateAttachmentInRequestConversion(requestConversionEntity, fileKey, converted, fileHash)
                                )
                )
                .filter(tuples -> tuples.getT2().getValue())
                .flatMap(tuples -> {
                    PdfConversionEntity pdfConversionEntity = tuples.getT1();
                    RequestConversionEntity requestConversionEntity = tuples.getT2().getKey();
                    pdfConversionEntity.setExpiration(BigDecimal.valueOf(OffsetDateTime.now().plusDays(offsetDays).toInstant().getEpochSecond()));

                    return updateRequestConversionWithTransaction(requestConversionEntity, pdfConversionEntity)
                            .thenReturn(Map.entry(convertToDto(requestConversionEntity), true));
                });
    }


    private Mono<Void> executeTransactionWithAsyncClient(List<TransactWriteItem> transactionItems) {
        TransactWriteItemsRequest transactWriteItemsRequest = TransactWriteItemsRequest.builder()
                .transactItems(transactionItems)
                .build();
        return Mono.fromCompletionStage(() -> dynamoDbAsyncClient.transactWriteItems(transactWriteItemsRequest)).then();
    }



}
