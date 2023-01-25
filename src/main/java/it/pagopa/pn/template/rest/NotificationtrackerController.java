package it.pagopa.pn.template.rest;




import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import it.pagopa.pn.template.notificationtracker.model.ResponseModel;
import it.pagopa.pn.template.notificationtracker.service.impl.NotificationtrackerServiceImpl;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationtrackerController   {

    private final  NotificationtrackerServiceImpl service;
    public  ResponseModel getSMSStatus(String process, String status, String clientId, String nextStatus) throws Exception {

     ResponseModel resul = service.getValidateStato(process,status,clientId,nextStatus);
	 return resul;
 }

    public  ResponseModel getEmailStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        ResponseModel resul = service.getValidateStato(process,status,clientId,nextStatus);
        return resul;
    }

    public  ResponseModel getPecStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        ResponseModel resul = service.getValidateStato(process,status,clientId,nextStatus);
        return resul;
    }

    public  ResponseModel getCartacepStatus(String process, String status, String clientId, String nextStatus) throws Exception {

        ResponseModel resul = service.getValidateStato(process,status,clientId,nextStatus);
        return resul;
    }

}
	  

