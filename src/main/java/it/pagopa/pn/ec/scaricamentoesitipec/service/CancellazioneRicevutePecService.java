package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CancellazioneRicevutePecDto;
import it.pagopa.pn.library.pec.service.PnPecService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.Semaphore;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.RequestUtils.concatRequestId;

@Service
@CustomLog
public class CancellazioneRicevutePecService {

    private final PnPecService pnPecService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final Semaphore semaphore;

    public CancellazioneRicevutePecService(PnPecService pnPecService, GestoreRepositoryCall gestoreRepositoryCall, @Value("${cancellazione-ricevute-pec.max-thread-pool-size}") Integer maxThreadPoolSize) {
        this.pnPecService = pnPecService;
        this.gestoreRepositoryCall = gestoreRepositoryCall;
        log.debug("{} max thread pool size : {} ", CANCELLAZIONE_RICEVUTE_PEC, maxThreadPoolSize);
        this.semaphore = new Semaphore(maxThreadPoolSize);
    }

    @SqsListener(value = "${cancellazione-ricevute-pec.sqs-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void cancellazioneRicevutePecInteractive(final CancellazioneRicevutePecDto cancellazioneRicevutePecDto, Acknowledgment acknowledgment) {
        var requestId = cancellazioneRicevutePecDto.getSingleStatusUpdate().getDigitalLegal().getRequestId();
        var clientId = cancellazioneRicevutePecDto.getSingleStatusUpdate().getClientId();
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, concatRequestId(clientId, requestId));
        log.logStartingProcess(CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE);
        MDCUtils.addMDCToContextAndExecute(cancellazioneRicevutePec(cancellazioneRicevutePecDto, requestId, acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE))
                .doOnError(throwable -> log.logEndingProcess(CANCELLAZIONE_RICEVUTE_PEC_INTERACTIVE, false, throwable.getMessage())))
                .subscribe();
    }

    public Mono<Void> cancellazioneRicevutePec(final CancellazioneRicevutePecDto cancellazioneRicevutePecDto, String requestId, Acknowledgment acknowledgment) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, CANCELLAZIONE_RICEVUTE_PEC, cancellazioneRicevutePecDto);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Mono.just(cancellazioneRicevutePecDto.getSingleStatusUpdate())
                .zipWhen(singleStatusUpdate -> gestoreRepositoryCall.getRichiesta(singleStatusUpdate.getClientId(), singleStatusUpdate.getDigitalLegal().getRequestId()))
                .flatMap(tuple -> {
                    var digitalLegal = tuple.getT1().getDigitalLegal();
                    var requestDto = tuple.getT2();
                    return Mono.justOrEmpty(requestDto.getRequestMetadata())
                            .flatMapMany(requestMetadataDto -> Flux.fromIterable(requestMetadataDto.getEventsList()))
                            .map(EventsDto::getDigProgrStatus)
                            .filter(digitalProgressStatusDto -> digitalLegal.getEventCode().getValue().equals(digitalProgressStatusDto.getStatusCode()))
                            .next()
                            .doOnSuccess(digitalProgressStatusDto -> {
                                if (digitalProgressStatusDto == null)
                                    log.warn(NOT_VALID_FOR_DELETE, requestId);
                            });
                })
                .map(digitalProgressStatusDto -> digitalProgressStatusDto.getGeneratedMessage().getId())
                .flatMap(pnPecService::deleteMessage)
                .doOnError(throwable -> log.fatal(CANCELLAZIONE_RICEVUTE_PEC, throwable, throwable.getMessage()))
                .doOnSuccess(result -> acknowledgment.acknowledge())
                .doFinally(signalType -> semaphore.release());
    }

}
