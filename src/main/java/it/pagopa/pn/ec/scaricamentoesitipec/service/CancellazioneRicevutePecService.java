package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.CancellazioneRicevutePecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CancellazioneRicevutePecDto;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

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
        var requestId = cancellazioneRicevutePecDto.getSingleStatusUpdate().getDigitalLegal().getRequestId();
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, requestId);
        log.info(STARTING_PROCESS_ON_LABEL, CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE, requestId);
        cancellazioneRicevutePec(cancellazioneRicevutePecDto, requestId, acknowledgment)
                .doOnSuccess(result -> log.info(ENDING_PROCESS_ON_LABEL, CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE, requestId))
                .doOnError(throwable -> log.warn(ENDING_PROCESS_ON_WITH_ERROR_LABEL, CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE, requestId, throwable, throwable.getMessage()))
                .subscribe();
    }

    public Mono<Void> cancellazioneRicevutePec(final CancellazioneRicevutePecDto cancellazioneRicevutePecDto, String requestId, Acknowledgment acknowledgment) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, CANCELLAZIONE_RICEVUTE_PEC, cancellazioneRicevutePecDto);
        return Mono.just(cancellazioneRicevutePecDto.getSingleStatusUpdate())
                .zipWhen(singleStatusUpdate -> gestoreRepositoryCall.getRichiesta(singleStatusUpdate.getClientId(), singleStatusUpdate.getDigitalLegal().getRequestId()))
                .flatMap(tuple -> {
                    var digitalLegal = tuple.getT1().getDigitalLegal();
                    var requestDto = tuple.getT2();
                    return Mono.justOrEmpty(requestDto.getRequestMetadata())
                            .flatMapMany(requestMetadataDto -> Flux.fromIterable(requestMetadataDto.getEventsList()))
                            .map(EventsDto::getDigProgrStatus)
                            .filter(digitalProgressStatusDto -> digitalLegal.getEventCode().getValue().equals(digitalProgressStatusDto.getStatusCode()))
                            .doOnDiscard(DigitalProgressStatusDto.class, digitalProgressStatusDto -> log.debug("Discarded status : {}", digitalProgressStatusDto))
                            .next()
                            .doOnSuccess(digitalProgressStatusDto -> {
                                if (digitalProgressStatusDto == null)
                                    log.warn(NOT_VALID_FOR_DELETE, requestId);
                            });
                })
                .map(digitalProgressStatusDto -> digitalProgressStatusDto.getGeneratedMessage().getId())
                .flatMap(messageID -> pnPecService.deleteMessage(messageID))
                .doOnError(throwable -> log.error(FATAL_IN_PROCESS_FOR, CANCELLAZIONE_RICEVUTE_PEC, requestId, throwable, throwable.getMessage()))
                .doOnSuccess(result -> acknowledgment.acknowledge());
    }

}
