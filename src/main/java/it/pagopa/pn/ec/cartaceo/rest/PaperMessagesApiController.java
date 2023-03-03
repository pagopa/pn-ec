package it.pagopa.pn.ec.cartaceo.rest;


import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.service.CartaceoService;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.rest.v1.api.PaperMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
public class PaperMessagesApiController implements PaperMessagesApi {


    private final  CartaceoService cartaceoService;

    @Autowired
    private StatusPullService paperService;

    public PaperMessagesApiController(CartaceoService cartaceoService) {
        this.cartaceoService = cartaceoService;
    }


    @Override
    public Mono<ResponseEntity<Void>> sendPaperEngageRequest(String requestIdx, String xPagopaExtchCxId, Mono<PaperEngageRequest> paperEngageRequest, ServerWebExchange exchange) {
        return paperEngageRequest.doOnNext(request -> log.info("<-- Start presa in Cartaceo -->"))
                .flatMap(request -> cartaceoService.presaInCarico(new CartaceoPresaInCaricoInfo(requestIdx,
                        xPagopaExtchCxId,
                        request)))
                .thenReturn(new ResponseEntity<>(OK));
    }





    @Override
    public Mono<ResponseEntity<PaperProgressStatusEvent>> getPaperEngageProgresses(String requestIdx,
                                                                                   String xPagopaExtchCxId, ServerWebExchange exchange) {
        return paperService.paperPullService(requestIdx, xPagopaExtchCxId).map(ResponseEntity::ok);
    }

}