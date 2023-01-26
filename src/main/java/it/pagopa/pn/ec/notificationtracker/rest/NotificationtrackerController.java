package it.pagopa.pn.ec.notificationtracker.rest;




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
    public Mono<Void> getStatoSmS(String process, String status, String clientId, String nextStatus) throws Exception {

        return service.getValidateStatoSmS(process,status,clientId,nextStatus);

 }

    public Mono<Void> getEmailStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        return service.getValidateStatoEmail(process,status,clientId,nextStatus);
    }

    public Mono<Void> getPecStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        return service.getValidateStatoPec(process,status,clientId,nextStatus);
    }

    public Mono<Void> getCartaceoStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        return service.getValidateCartaceStatus(process,status,clientId,nextStatus);

    }

    public Mono<Void> putStatoSmS(String process, String nextStatus, String clientId){
        return service.putStatoSms(process,nextStatus,clientId);
    }

    public Mono<Void> putStatoEmail(String process, String nextStatus, String clientId){
        return service.putStatoEmail(process,nextStatus,clientId);
    }

    public Mono<Void> putStatoPec(String process, String nextStatus, String clientId){
        return service.putStatoPec(process,nextStatus,clientId);
    }

    public Mono<Void> putStatoCartaceo(String process, String nextStatus, String clientId){
        return service.putStatoCartaceo(process,nextStatus,clientId);
    }

}
	  

