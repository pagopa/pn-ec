package it.pagopa.pn.ec.email.rest;


import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_MAIL;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
//@RestController
public class DigitalCourtesyMessagesEmailApiController implements DigitalCourtesyMessagesApi {


    private final EmailService service;

    public DigitalCourtesyMessagesEmailApiController(EmailService service) {
        this.service = service;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalCourtesyMessage(String requestIdx, String xPagopaExtchCxId,
                                                                  Mono<DigitalCourtesyMailRequest>  digitalCourtesyMailRequest,
                                                                  final ServerWebExchange exchange){

        return digitalCourtesyMailRequest.doOnNext(request -> log.info("<-- Start presa in email -->"))
        .flatMap(request -> service.presaInCarico(new EmailPresaInCaricoInfo(requestIdx,
                        xPagopaExtchCxId,
                        request)))
                .then(Mono.just(new ResponseEntity<>(OK)));
    }



}
