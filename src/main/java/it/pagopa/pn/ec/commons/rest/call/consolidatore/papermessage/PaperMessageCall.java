package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import org.openapitools.client.model.*;
import reactor.core.publisher.Mono;

public interface PaperMessageCall {

//  <-- ENGAGEMENT -->
    Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest);
    Mono<OperationResultCodeResponse> putDuplicateRequest(PaperReplicaRequest paperEngageRequest);

//  <-- PROGRESS -->
    Mono<PaperDeliveryProgressesResponse> getProgress(String requestId);
    Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId);
}
