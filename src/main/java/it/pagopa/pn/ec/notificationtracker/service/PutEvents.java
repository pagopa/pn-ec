package it.pagopa.pn.ec.notificationtracker.service;

import it.pagopa.pn.ec.notificationtracker.model.NotificationRequestModel;
import reactor.core.publisher.Mono;

public interface PutEvents {

    Mono<Void> putEventExternal(NotificationRequestModel eventInfo) throws Exception;
}
