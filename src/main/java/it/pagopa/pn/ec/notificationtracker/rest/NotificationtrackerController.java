package it.pagopa.pn.ec.notificationtracker.rest;




import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import it.pagopa.pn.ec.notificationtracker.service.impl.NotificationtrackerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationtrackerController   {

    private final  NotificationtrackerServiceImpl service;
    public Mono<Void> getStatoSmS( final NotificationTrackerQueueDto message)  {
        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        return service.getValidateStatoSmS(message);

 }

    public Mono<Void> getEmailStatus(final NotificationTrackerQueueDto message)  {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL -->");
        return service.getValidateStatoEmail(message);
    }

    public Mono<Void> getPecStatus(final NotificationTrackerQueueDto message)  {
        log.info("<-- START LAVORAZIONE RICHIESTA PEC -->");
        return service.getValidateStatoPec(message);
    }

    public Mono<Void> getCartaceoStatus(final NotificationTrackerQueueDto message)  {
        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO -->");
        return service.getValidateCartaceStatus(message);

    }


}
	  

