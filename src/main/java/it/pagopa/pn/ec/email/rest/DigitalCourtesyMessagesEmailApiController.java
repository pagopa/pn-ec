package it.pagopa.pn.ec.email.rest;


import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.service.StatusPullService;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.email.service.EmailService;
import it.pagopa.pn.ec.rest.v1.api.DigitalCourtesyMessagesApi;
import it.pagopa.pn.ec.rest.v1.dto.CourtesyMessageProgressEvent;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.repositorymanager.utils.RequestMapper.concatRequestId;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
//@RestController
public class DigitalCourtesyMessagesEmailApiController implements DigitalCourtesyMessagesApi {


    /*
     * Gli endpoint di SMS ed EMAIL sono state accorpati nello stesso tag OpenApi.
     * Ciò ha generato un'interfaccia Java comune e dato che all'interno dello
     * stesso contesto Spring non possono coesistere due @RequestController che
     * espongono lo stesso endpoint abbiamo dovuto implementare le API nello stesso
     * controller.
     */

    private final EmailService service;
    private final StatusPullService statusPullService;
    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

    public DigitalCourtesyMessagesEmailApiController(EmailService service, StatusPullService statusPullService,
                                                     TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.service = service;
        this.statusPullService = statusPullService;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Override
    public Mono<ResponseEntity<Void>> sendDigitalCourtesyMessage(String requestIdx, String xPagopaExtchCxId,
                                                                 Mono<DigitalCourtesyMailRequest> digitalCourtesyMailRequest,
                                                                 final ServerWebExchange exchange) {
        return digitalCourtesyMailRequest.flatMap(request -> service.presaInCarico(EmailPresaInCaricoInfo.builder()
                                                                                                         .requestIdx(requestIdx)
                                                                                                         .xPagopaExtchCxId(xPagopaExtchCxId)
                                                                                                         .digitalCourtesyMailRequest(request)
                                                                                                         .build()))
                                         .then(Mono.just(new ResponseEntity<>(OK)));
    }

    @Override
    public Mono<ResponseEntity<CourtesyMessageProgressEvent>> getDigitalCourtesyMessageStatus(String requestIdx, String xPagopaExtchCxId,
                                                                                              ServerWebExchange exchange) {
        return statusPullService.digitalPullService(requestIdx, xPagopaExtchCxId, transactionProcessConfigurationProperties.email())
                                .map(ResponseEntity::ok);
    }
}
