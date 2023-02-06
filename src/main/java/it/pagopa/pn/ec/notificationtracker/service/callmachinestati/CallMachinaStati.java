package it.pagopa.pn.ec.notificationtracker.service.callmachinestati;

import it.pagopa.pn.ec.notificationtracker.model.NotificationResponseModel;
import reactor.core.publisher.Mono;

public interface CallMachinaStati {

    Mono<NotificationResponseModel>  getStato(String prossesId, String currStatus, String clientId, String nextStatus);
}
