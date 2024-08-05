package it.pagopa.pn.ec.pdfraster.service.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
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
        return Mono.fromCallable(() -> convertToEntity(requestConversionDto))
                .flatMap(this::saveRequestConversionEntity)
                .flatMap(this::savePdfConversions)
                .map(this::convertToDto)
                .doOnSuccess(result -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION))
                .doOnError(throwable -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION, false, throwable.getMessage()));
    }

    private RequestConversionEntity convertToEntity(RequestConversionDto dto) {
        log.logStartingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION);
        return objectMapper.convertValue(dto, RequestConversionEntity.class);
    }

    private Mono<RequestConversionEntity> saveRequestConversionEntity(RequestConversionEntity entity) {
        return Mono.fromCallable(() -> {
            requestTable.putItem(PutItemEnhancedRequest.builder(RequestConversionEntity.class)
                    .item(entity)
                    .build());
            return entity;
        });
    }

    private Mono<RequestConversionEntity> savePdfConversions(RequestConversionEntity entity) {
        String requestId = entity.getRequestId();
        return Flux.fromIterable(entity.getAttachments())
                .map(attachment -> createPdfConversion(attachment, requestId))
                .flatMap(this::savePdfConversion)
                .then(Mono.just(entity));
    }

    private PdfConversionEntity createPdfConversion(AttachmentToConvert attachment, String requestId) {
        PdfConversionEntity pdfConversion = new PdfConversionEntity();
        pdfConversion.setFileKey(attachment.getNewFileKey().replace("safestorage://", ""));
        pdfConversion.setRequestId(requestId);
        pdfConversion.setExpiration(null);
        return pdfConversion;
    }

    private Mono<Void> savePdfConversion(PdfConversionEntity pdfConversion) {
        return Mono.fromCallable(() -> {
            conversionTable.putItem(PutItemEnhancedRequest.builder(PdfConversionEntity.class)
                    .item(pdfConversion)
                    .build());
            return null;
        });
    }

    private RequestConversionDto convertToDto(RequestConversionEntity entity) {
        return objectMapper.convertValue(entity, RequestConversionDto.class);
    }


    @Override
    public Mono<RequestConversionDto> updateRequestConversion(String fileKey, Boolean converted) {
        log.logStartingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION);

        if (converted == null || !converted) {
            return Mono.error(new IllegalArgumentException("Invalid value for 'converted': must be true."));
        }

        return getPdfConversionFromDynamoDb(fileKey)
                .switchIfEmpty(Mono.error(new NotFoundException("Not found for fileKey: " + fileKey)))
                .flatMap(pdfConversionEntity -> getRequestConversionFromDynamoDb(pdfConversionEntity.getRequestId())
                        .flatMap(requestConversionEntity -> updateAttachmentConversion(requestConversionEntity, fileKey, converted))
                        .map(this::convertToDto)
                        .doOnSuccess(result -> log.logEndingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION))
                        .doOnError(exception -> log.logEndingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION, false, exception.getMessage()))
                );
    }

    private Mono<RequestConversionEntity> updateAttachmentConversion(RequestConversionEntity requestConversionEntity, String fileKey, Boolean converted) {
        requestConversionEntity.getAttachments().stream()
                .filter(attachment -> attachment.getNewFileKey().equals(fileKey))
                .findFirst()
                .ifPresent(attachment -> attachment.setConverted(converted));

        return Mono.fromFuture(requestTable.putItem(requestConversionEntity))
                .thenReturn(requestConversionEntity);
    }

    private Mono<RequestConversionEntity> getRequestConversionFromDynamoDb(String requestId) {
        return Mono.fromCompletionStage(() -> requestTable.getItem(Key.builder().partitionValue(requestId).build()));
    }

    private Mono<PdfConversionEntity> getPdfConversionFromDynamoDb(String fileKey) {
        return Mono.fromCompletionStage(() -> conversionTable.getItem(Key.builder().partitionValue(fileKey).build()));
    }


}
