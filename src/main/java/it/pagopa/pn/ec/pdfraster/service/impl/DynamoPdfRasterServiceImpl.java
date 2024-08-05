package it.pagopa.pn.ec.pdfraster.service.impl;

import com.amazonaws.services.kms.model.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.pdfraster.model.entity.PdfConversionEntity;
import it.pagopa.pn.ec.pdfraster.model.entity.RequestConversionEntity;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.rest.v1.dto.PdfConversionDto;
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
        return Mono.fromCallable(() -> {
                    log.logStartingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION);
                    RequestConversionEntity requestConversionEntity = objectMapper.convertValue(requestConversionDto, RequestConversionEntity.class);

                    requestTable.putItem(PutItemEnhancedRequest.builder(RequestConversionEntity.class)
                            .item(requestConversionEntity)
                            .build());
                    return requestConversionEntity;
                })
                .flatMap(requestConversionEntity -> {
                    Flux<PdfConversionEntity> pdfConversions = Flux.fromIterable(requestConversionEntity.getAttachments())
                            .map(attachment -> {
                                PdfConversionEntity pdfConversion = new PdfConversionEntity();
                                pdfConversion.setFileKey(attachment.getNewFileKey().replace("safestorage://", ""));
                                pdfConversion.setRequestId(requestConversionEntity.getRequestId());
                                pdfConversion.setExpiration(null);
                                return pdfConversion;
                            });
                    return Flux.from(pdfConversions)
                            .flatMap(pdfConversion -> Mono.fromCallable(() -> conversionTable.putItem(PutItemEnhancedRequest.builder(PdfConversionEntity.class)
                                    .item(pdfConversion)
                                    .build())))
                            .then(Mono.just(requestConversionEntity));
                })
                .map(requestConversionEntity -> objectMapper.convertValue(requestConversionEntity, RequestConversionDto.class))
                .doOnSuccess(result -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION))
                .doOnError(throwable -> log.logEndingProcess(PDF_RASTER_INSERT_REQUEST_CONVERSION, false, throwable.getMessage()));
    }


    @Override
    public Mono<RequestConversionDto> updateRequestConversion(String fileKey, Boolean converted) {

        log.logStartingProcess(PDF_RASTER_UPDATE_REQUEST_CONVERSION);

        if (converted == null || !converted) {
            return Mono.error(new IllegalArgumentException("Invalid value for 'converted': must be true."));
        }

        return getPdfConversionFromDynamoDb(fileKey)
                .map(pdfConversionEntity ->
                        objectMapper.convertValue(pdfConversionEntity, PdfConversionDto.class)
                )
                .switchIfEmpty(Mono.error(new NotFoundException("Not found for fileKey: " + fileKey)))
                .flatMap(existingRequest -> {

                    String requestId = existingRequest.getRequestId();
                    return getRequestConversionFromDynamoDb(requestId);

                })
                .flatMap(requestConversionEntity -> {
                    requestConversionEntity.getAttachments().parallelStream()
                            .filter(attachmentToConvert -> attachmentToConvert.getNewFileKey().equals(fileKey))
                            .findFirst()
                            .ifPresent(attachmentToConvert -> attachmentToConvert.setConverted(true));


                    return Mono.fromFuture(requestTable.putItem(requestConversionEntity))
                            .thenReturn(requestConversionEntity);
                })

                .map(requestConversionEntity -> objectMapper.convertValue(requestConversionEntity, RequestConversionDto.class))
                .doOnSuccess(result -> log.logEndingProcess(GESTIONE_RETRY_CARTACEO))
                .doOnError(exception -> log.logEndingProcess(GESTIONE_RETRY_CARTACEO, false, exception.getMessage()));

    }

    private Mono<RequestConversionEntity> getRequestConversionFromDynamoDb(String requestId) {
        return Mono.fromCompletionStage(() -> requestTable.getItem(Key.builder().partitionValue(requestId).build()));
    }

    private Mono<PdfConversionEntity> getPdfConversionFromDynamoDb(String fileKey) {
        return Mono.fromCompletionStage(() -> conversionTable.getItem(Key.builder().partitionValue(fileKey).build()));
    }


}
