package it.pagopa.pn.ec.notificationtracker.rest;




import io.awspring.cloud.messaging.listener.Acknowledgment;
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
    public Mono<Void> getStatoSmS(String process, String status, String xpagopaExtchCxId, String nextStatus)  {
        log.info("<-- START LAVORAZIONE RICHIESTA SMS -->");
        return service.getValidateStatoSmS(process,status,xpagopaExtchCxId,nextStatus);

 }

    public Mono<Void> getEmailStatus(String process, String status, String xpagopaExtchCxId, String nextStatus)  {
        log.info("<-- START LAVORAZIONE RICHIESTA EMAIL -->");
        return service.getValidateStatoEmail(process,status,xpagopaExtchCxId,nextStatus);
    }

    public Mono<Void> getPecStatus(String process, String status, String xpagopaExtchCxId, String nextStatus)  {
        log.info("<-- START LAVORAZIONE RICHIESTA PEC -->");
        return service.getValidateStatoPec(process,status,xpagopaExtchCxId,nextStatus);
    }

    public Mono<Void> getCartaceoStatus(String process, String status, String xpagopaExtchCxId, String nextStatus)  {
        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO -->");
        return service.getValidateCartaceStatus(process,status,xpagopaExtchCxId,nextStatus);

    }


}
	  

