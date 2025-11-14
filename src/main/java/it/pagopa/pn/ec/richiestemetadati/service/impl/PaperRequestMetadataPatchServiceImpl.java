package it.pagopa.pn.ec.richiestemetadati.service.impl;

import it.pagopa.pn.commons.utils.dynamodb.async.DynamoDbAsyncTableDecorator;
import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.commons.utils.RequestUtils;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataPatchRequest;
import it.pagopa.pn.ec.richiestemetadati.service.PaperRequestMetadataPatchService;
import lombok.CustomLog;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;


@Service
@CustomLog
public class PaperRequestMetadataPatchServiceImpl implements PaperRequestMetadataPatchService {

    private final DynamoDbAsyncTableDecorator<RequestMetadata> requestMetadataDynamoDbAsyncTableDecorator;

    public PaperRequestMetadataPatchServiceImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        TableSchema<RequestMetadata> requestMetadataTableSchema = TableSchema.fromBean(RequestMetadata.class);
        this.requestMetadataDynamoDbAsyncTableDecorator = new DynamoDbAsyncTableDecorator<>(dynamoDbEnhancedAsyncClient.table(repositoryManagerDynamoTableName.richiesteMetadataName(), requestMetadataTableSchema));
    }


    @Override
    public Mono<Void> patchIsOpenReworkRequest(String xPagopaExtchCxId,String requestId, RequestMetadataPatchRequest req) {
       String id = RequestUtils.concatRequestId(xPagopaExtchCxId,requestId);
        log.logStartingProcess(PAPER_REQUEST_METADATA_PATCH_SERVICE_PATCH_IS_OPEN_REWORK_REQUEST);
        if (req == null) {
            return Mono.error(new RepositoryManagerException.RequestMalformedException(PAPER_REQUEST_METADATA_PATCH_SERVICE_PATCH_IS_OPEN_REWORK_REQUEST));
        }
        if (Strings.isBlank(requestId)) {
            return Mono.error(new RepositoryManagerException.RequestMalformedException(PAPER_REQUEST_METADATA_PATCH_SERVICE_PATCH_IS_OPEN_REWORK_REQUEST));
        }

        return processUpdatePaperRequestMetadataIsOpenReworkRequest(id, req)
                .doOnSuccess(result -> log.info(PAPER_REQUEST_METADATA_PATCH_SERVICE_PATCH_IS_OPEN_REWORK_REQUEST))
                .doOnError(exception -> log.logEndingProcess(PAPER_REQUEST_METADATA_PATCH_SERVICE_PATCH_IS_OPEN_REWORK_REQUEST, false, exception.getMessage())).then()
                ;

    }

    private Mono<Object> processUpdatePaperRequestMetadataIsOpenReworkRequest(String id, RequestMetadataPatchRequest req) {
        log.debug("id:{},req:{}",id,req.toString());
        return getRequestMetadata(id)
                .flatMap(requestMetadata -> {
                    if (requestMetadata.getPaperRequestMetadata() == null){
                        return Mono.error(
                                new RepositoryManagerException.RequestMalformedException(
                                        "PaperRequestMetadata is missing for request: " + id
                                ));

                    }
                    return applyChange(requestMetadata, req);
                })
                .flatMap(this::updateRequestMetadata);
    }

    private Mono<RequestMetadata> getRequestMetadata(String id) {
        log.debug("id:{}",id);
        return Mono.fromCompletionStage(() -> requestMetadataDynamoDbAsyncTableDecorator.getItem(Key.builder().partitionValue(id).build()))
                .switchIfEmpty(Mono.error(new RepositoryManagerException.RequestNotFoundException(id)))
                .doOnSuccess(result -> log.info(PAPER_REQUEST_METADATA_PATCH_SERVICE_GET_REQUEST_METADATA));
    }

    private Mono<RequestMetadata> applyChange(RequestMetadata requestMetadata, RequestMetadataPatchRequest req) {
        log.debug("requestMetadata:{},patch request:{}",requestMetadata.toString(),req.toString());
        requestMetadata.getPaperRequestMetadata().setIsOpenReworkRequest(req.getIsOpenReworkRequest());
        return Mono.just(requestMetadata);
    }

    private Mono<RequestMetadata> updateRequestMetadata(RequestMetadata requestMetadata) {
        log.debug("requestMetadata:{}",requestMetadata);
        return Mono.fromCompletionStage(requestMetadataDynamoDbAsyncTableDecorator.updateItem(UpdateItemEnhancedRequest.builder(RequestMetadata.class)
                .item(requestMetadata)
                .conditionExpression(Expression.builder().build())
                .build()))
                .doOnSuccess(result -> log.info(PAPER_REQUEST_METADATA_PATCH_SERVICE_UPDATE_REQUEST_METADATA));
    }


}
