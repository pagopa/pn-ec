package it.pagopa.pn.ec.notificationtracker.rest;


import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationTrackerServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationTrackerController {

    private final NotificationTrackerServiceImpl service;

    public Mono<Void> getStatoSmS(final NotificationTrackerQueueDto message) {
        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        return service.getValidateStatoSmS(message);

    }

    public Mono<Void> getEmailStatus(final NotificationTrackerQueueDto message) {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL -->");
        return service.getValidateStatoEmail(message);
    }

    public Mono<Void> getPecStatus(final NotificationTrackerQueueDto message) {
        log.info("<-- START LAVORAZIONE RICHIESTA PEC -->");
        return service.getValidateStatoPec(message);
    }

    public Mono<Void> getCartaceoStatus(final NotificationTrackerQueueDto message) {
        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO -->");
        return service.getValidateCartaceStatus(message);

    }
}
