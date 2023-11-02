package it.pagopa.pn.ec.scaricamentoesitipec.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataDto;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CancellazioneRicevutePecDto;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@Service
@Slf4j
public class CancellazioneRicevutePecService {

    @Autowired
    private PnPecService pnPecService;
    @Autowired
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private ArubaSecretValue arubaSecretValue;



    public Mono<Void> cancellazioneRicevutePec(final CancellazioneRicevutePecDto cancellazioneRicevutePecDto, Acknowledgment acknowledgment) {
        log.info(STARTING_SCHEDULED, CANCELLAZIONE_RICEVUTE_PEC);

        var requestId = cancellazioneRicevutePecDto.getSingleStatusUpdate().getDigitalLegal().getRequestId();
        return Mono.just(cancellazioneRicevutePecDto.getSingleStatusUpdate())
                .zipWhen(singleStatusUpdate -> gestoreRepositoryCall.getRichiesta(singleStatusUpdate.getClientId(), singleStatusUpdate.getDigitalLegal().getRequestId()))
                .flatMap(tuple -> {
                    var digitalLegal = tuple.getT1().getDigitalLegal();
                    var requestDto = tuple.getT2();
                    return Mono.justOrEmpty(requestDto.getRequestMetadata())
                            .flatMapMany(requestMetadataDto -> Flux.fromIterable(requestMetadataDto.getEventsList()))
                            .map(EventsDto::getDigProgrStatus)
                            .filter(digitalProgressStatusDto -> digitalLegal.getEventCode().getValue().equals(digitalProgressStatusDto.getStatusCode()))
                            .doOnDiscard(DigitalProgressStatusDto.class, digitalProgressStatusDto -> log.info("Discarded status : {}", digitalProgressStatusDto))
                            .next()
                            .doOnSuccess(digitalProgressStatusDto -> {
                                if (digitalProgressStatusDto == null)
                                    log.warn(NOT_VALID_FOR_DELETE, requestId);
                            });
                })
                .map(digitalProgressStatusDto -> digitalProgressStatusDto.getGeneratedMessage().getId())
                .flatMap(messageID -> pnPecService.deleteMessage(messageID))
                .doOnError(throwable -> log.error(FATAL_IN_PROCESS, CANCELLAZIONE_RICEVUTE_PEC, throwable, throwable.getMessage()))
                .doOnSuccess(result -> acknowledgment.acknowledge());
    }

}
