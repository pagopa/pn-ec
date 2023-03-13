package it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage;

import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import org.openapitools.client.model.*;
import reactor.core.publisher.Mono;

public interface PaperMessageCall {

//  <-- ENGAGEMENT -->
    Mono<OperationResultCodeResponse> putRequest(PaperEngageRequest paperEngageRequest) throws RestCallException.ResourceAlreadyInProgressException;
    Mono<OperationResultCodeResponse> putDuplicateRequest(PaperReplicaRequest paperReplicaRequest) throws RestCallException.ResourceAlreadyInProgressException;

//  <-- PROGRESS -->
    Mono<PaperDeliveryProgressesResponse> getProgress(String requestId) throws RestCallException.ResourceNotFoundException;
    Mono<PaperReplicasProgressesResponse> getDuplicateProgress(String requestId) throws RestCallException.ResourceNotFoundException;
}
