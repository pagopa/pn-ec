package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.CancellazioneRicevutePecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CancellazioneRicevutePecDto;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;

@Service
@Slf4j
public class CancellazioneRicevutePecService {

    @Autowired
    private PnPecService pnPecService;
    @Autowired
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Autowired
    private ArubaSecretValue arubaSecretValue;
    @Autowired
    private CancellazioneRicevutePecProperties cancellazioneRicevutePecProperties;

    @SqsListener(value = "${cancellazione-ricevute-pec.sqs-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void cancellazioneRicevutePecInteractive(final CancellazioneRicevutePecDto cancellazioneRicevutePecDto, Acknowledgment acknowledgment) {
        logIncomingMessage(cancellazioneRicevutePecProperties.sqsQueueName(), cancellazioneRicevutePecDto);
        cancellazioneRicevutePec(cancellazioneRicevutePecDto, acknowledgment).subscribe();
    }

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
