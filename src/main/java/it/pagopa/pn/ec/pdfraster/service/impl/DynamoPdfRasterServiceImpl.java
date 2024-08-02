package it.pagopa.pn.ec.pdfraster.service.impl;

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
                .doOnSuccess(result -> log.info("Successfully inserted request conversion with ID: {}", result.getRequestId()))
                .doOnError(error -> log.error("Failed to insert request conversion", error));
    }




    @Override
    public Mono<RequestConversionDto> updateRequestConversion(String fileKey, Boolean converted) {

        if (converted == null || !converted) {
            return Mono.error(new IllegalArgumentException("Invalid value for 'converted': must be true."));
        }

        return getPdfConversionFromDynamoDb(fileKey)
                .map(pdfConversionEntity ->
                        objectMapper.convertValue(pdfConversionEntity, PdfConversionDto.class)
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Not found for fileKey: " + fileKey)))
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
                .doOnSuccess(result -> log.info("Update successful with fileKey: {}", fileKey))
                .onErrorResume(e -> {
                    log.error("Error updating request conversion with fileKey: {}", fileKey, e);
                    return Mono.error(new RuntimeException("Error updating request conversion", e));
                });
    }

    private Mono<RequestConversionEntity> getRequestConversionFromDynamoDb(String requestId) {
        return Mono.fromCompletionStage(() -> requestTable.getItem(Key.builder().partitionValue(requestId).build()));
    }

    private Mono<PdfConversionEntity> getPdfConversionFromDynamoDb(String fileKey) {
        return Mono.fromCompletionStage(() -> conversionTable.getItem(Key.builder().partitionValue(fileKey).build()));
    }




}
