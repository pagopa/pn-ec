package it.pagopa.pn.ec.commons.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public interface SnsService {

    Mono<PublishResponse> send(String phoneNumber, String message);

}
