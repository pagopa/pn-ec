package it.pagopa.pn.ec.sercq.service.impl;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.QueueOperationsService;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


@Service
@CustomLog
public class SercqService extends PresaInCaricoService implements QueueOperationsService {

    protected SercqService(AuthService authService) {
        super(authService);
    }



    @Override
    public Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {

        //simile a quella di pec
        return null;
    }

    public Mono<RequestDto> insertRequestFromSercq(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId) {
        return Mono.empty();
    }

    @Override
    public Mono<SendMessageResponse> sendNotificationOnStatusQueue(PresaInCaricoInfo presaInCaricoInfo, String status,
                                                                   DigitalProgressStatusDto digitalProgressStatusDto) {
        return Mono.empty();
    }
}
