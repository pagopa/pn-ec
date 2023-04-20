package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.mapper.CartaceoMapper;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.StatusToDeleteException;
import it.pagopa.pn.ec.commons.exception.cartaceo.CartaceoSendException;
import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.policy.Policy;
import it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoPaper;
import static it.pagopa.pn.ec.commons.rest.call.consolidatore.papermessage.PaperMessageCall.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.ReactorUtils.pullFromMonoUntilIsEmpty;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.consolidatore.utils.PaperResult.CODE_TO_STATUS_MAP;

@Service
@Slf4j
public class CartaceoService extends PresaInCaricoService {


    private final SqsService sqsService;
    private final GestoreRepositoryCall gestoreRepositoryCall;
    private final AttachmentServiceImpl attachmentService;
    private final NotificationTrackerSqsName notificationTrackerSqsName;
    private final CartaceoSqsQueueName cartaceoSqsQueueName;
    private final PaperMessageCall paperMessageCall;
    private final CartaceoMapper cartaceoMapper;
    private String idSaved;

    protected CartaceoService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
                              GestoreRepositoryCall gestoreRepositoryCall1, AttachmentServiceImpl attachmentService,
                              NotificationTrackerSqsName notificationTrackerSqsName, CartaceoSqsQueueName cartaceoSqsQueueName,
                              PaperMessageCall paperMessageCall, CartaceoMapper cartaceoMapper) {
        super(authService, gestoreRepositoryCall);
        this.sqsService = sqsService;
        this.gestoreRepositoryCall = gestoreRepositoryCall1;
        this.attachmentService = attachmentService;
        this.notificationTrackerSqsName = notificationTrackerSqsName;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.paperMessageCall = paperMessageCall;
        this.cartaceoMapper = cartaceoMapper;
    }

    @Override
    protected Mono<Void> specificPresaInCarico(PresaInCaricoInfo presaInCaricoInfo) {
        log.info("<-- START PRESA IN CARICO CARTACEO --> Request ID: {}, Client ID: {}",
                 presaInCaricoInfo.getRequestIdx(),
                 presaInCaricoInfo.getXPagopaExtchCxId());

        var cartaceoPresaInCaricoInfo = (CartaceoPresaInCaricoInfo) presaInCaricoInfo;
        var xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();
        List<String> attachmentsUri = getPaperUri(cartaceoPresaInCaricoInfo.getPaperEngageRequest().getAttachments());
        var paperNotificationRequest = cartaceoPresaInCaricoInfo.getPaperEngageRequest();

        return attachmentService.getAllegatiPresignedUrlOrMetadata(attachmentsUri, presaInCaricoInfo.getXPagopaExtchCxId(), true)
                                .then(insertRequestFromCartaceo(paperNotificationRequest,
                                                                xPagopaExtchCxId).onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException())))

                                .flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                                       createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                                                                                              BOOKED.getStatusTransactionTableCompliant(),
                                                                                                              // TODO: SET MISSING
                                                                                                              //  PROPERTIES
                                                                                                              new PaperProgressStatusDto())))
//                              Publish to CARTACEO BATCH
                                .flatMap(sendMessageResponse -> {
                                    PaperEngageRequest req = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
                                    if (req != null) {
                                        return sqsService.send(cartaceoSqsQueueName.batchName(),
                                                               cartaceoPresaInCaricoInfo.getPaperEngageRequest());
                                    } else {
                                        return Mono.empty();
                                    }
                                })
                                .then();
    }

    private ArrayList<String> getPaperUri(List<PaperEngageRequestAttachments> paperEngageRequestAttachments) {
        ArrayList<String> list = new ArrayList<>();
        if (!paperEngageRequestAttachments.isEmpty()) {
            for (PaperEngageRequestAttachments attachment : paperEngageRequestAttachments) {
                list.add(attachment.getUri());
            }
        }
        return list;
    }

    private Mono<RequestDto> insertRequestFromCartaceo(PaperEngageRequest paperNotificationRequest, String xPagopaExtchCxId) {
        log.info("<-- START INSERT REQUEST FROM CARTACEO --> Request ID: {}, Client ID: {}",
                 paperNotificationRequest.getRequestId(),
                 xPagopaExtchCxId);
        return Mono.fromCallable(() -> {
            var requestDto = new RequestDto();
            requestDto.setRequestIdx(paperNotificationRequest.getRequestId());
            requestDto.setClientRequestTimeStamp(paperNotificationRequest.getClientRequestTimeStamp());
            requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);
            var requestPersonalDto = new RequestPersonalDto();
            var digitalRequestPersonalDto = new PaperRequestPersonalDto();

            List<AttachmentsEngageRequestDto> attachmentsEngageRequestDto = new ArrayList<>();
            if (!paperNotificationRequest.getAttachments().isEmpty()) {
                for (PaperEngageRequestAttachments attachment : paperNotificationRequest.getAttachments()) {
                    AttachmentsEngageRequestDto attachments = new AttachmentsEngageRequestDto();
                    attachments.setUri(attachment.getUri());
                    attachments.setOrder(attachment.getOrder());
                    attachments.setDocumentType(attachment.getDocumentType());
                    attachments.setSha256(attachment.getSha256());
                    attachmentsEngageRequestDto.add(attachments);

                }
            }

            digitalRequestPersonalDto.setAttachments(attachmentsEngageRequestDto);

            digitalRequestPersonalDto.setReceiverName(paperNotificationRequest.getReceiverName());
            digitalRequestPersonalDto.setReceiverNameRow2(paperNotificationRequest.getReceiverNameRow2());
            digitalRequestPersonalDto.setReceiverAddress(paperNotificationRequest.getReceiverAddress());
            digitalRequestPersonalDto.setReceiverAddressRow2(paperNotificationRequest.getReceiverAddressRow2());
            digitalRequestPersonalDto.setReceiverCap(paperNotificationRequest.getReceiverCap());
            digitalRequestPersonalDto.setReceiverCity(paperNotificationRequest.getReceiverCity());
            digitalRequestPersonalDto.setReceiverCity2(paperNotificationRequest.getReceiverCity2());
            digitalRequestPersonalDto.setReceiverPr(paperNotificationRequest.getReceiverPr());
            digitalRequestPersonalDto.setReceiverCountry(paperNotificationRequest.getReceiverCountry());
            digitalRequestPersonalDto.setReceiverFiscalCode(paperNotificationRequest.getReceiverFiscalCode());
            digitalRequestPersonalDto.setSenderName(paperNotificationRequest.getSenderName());
            digitalRequestPersonalDto.setSenderAddress(paperNotificationRequest.getSenderAddress());
            digitalRequestPersonalDto.setSenderCity(paperNotificationRequest.getSenderCity());
            digitalRequestPersonalDto.setSenderPr(paperNotificationRequest.getSenderPr());
            digitalRequestPersonalDto.setSenderDigitalAddress(paperNotificationRequest.getSenderDigitalAddress());
            digitalRequestPersonalDto.setArName(paperNotificationRequest.getArName());
            digitalRequestPersonalDto.setArAddress(paperNotificationRequest.getArAddress());
            digitalRequestPersonalDto.setArCap(paperNotificationRequest.getArCap());
            digitalRequestPersonalDto.setArCity(paperNotificationRequest.getArCity());
            requestPersonalDto.setPaperRequestPersonal(digitalRequestPersonalDto);

            var requestMetadataDto = new RequestMetadataDto();
            var digitalRequestMetadataDto = new PaperRequestMetadataDto();
            digitalRequestMetadataDto.setRequestPaId(paperNotificationRequest.getRequestPaId());
            digitalRequestMetadataDto.setIun(paperNotificationRequest.getIun());
            digitalRequestMetadataDto.setVas(paperNotificationRequest.getVas());
            digitalRequestMetadataDto.setPrintType(paperNotificationRequest.getPrintType());
            digitalRequestMetadataDto.setProductType(paperNotificationRequest.getProductType());
            requestMetadataDto.setPaperRequestMetadata(digitalRequestMetadataDto);

            requestDto.setRequestPersonal(requestPersonalDto);
            requestDto.setRequestMetadata(requestMetadataDto);
            return requestDto;
        }).flatMap(gestoreRepositoryCall::insertRichiesta);
    }

    @Scheduled(cron = "${cron.value.lavorazione-batch-cartaceo}")
    public void lavorazioneRichiestaBatch() {
        sqsService.getOneMessage(cartaceoSqsQueueName.batchName(), CartaceoPresaInCaricoInfo.class)//
                  .doOnNext(cartaceoPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(cartaceoSqsQueueName.batchName()//
                          , cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> Mono.zip(Mono.just(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessage())
//
                          , lavorazioneRichiesta(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent())))
                  .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> sqsService.deleteMessageFromQueue(
                          cartaceoPresaInCaricoInfoSqsMessageWrapper.getT1()
//
                          ,
                          cartaceoSqsQueueName.batchName()))
                  .transform(pullFromMonoUntilIsEmpty())//
                  .subscribe();
    }

    Mono<SendMessageResponse> lavorazioneRichiesta(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo) {

        log.info("<-- START LAVORAZIONE RICHIESTA CARTACEO --> Request ID : {}, Client ID : {}",
                 cartaceoPresaInCaricoInfo.getRequestIdx(),
                 cartaceoPresaInCaricoInfo.getXPagopaExtchCxId());

        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);

        return gestoreRepositoryCall.getRichiesta(cartaceoPresaInCaricoInfo.getXPagopaExtchCxId(),
                        cartaceoPresaInCaricoInfo.getRequestIdx()).flatMap(requestDto ->
                        // Try to send PAPER
                        paperMessageCall.putRequest(
                                        paperEngageRequestDst)
                                .retryWhen(
                                        DEFAULT_RETRY_STRATEGY)
                                // The PAPER
                                // in sent,
                                // publish
                                // to
                                // Notification Tracker with next status ->
                                // SENT
                                .flatMap(
                                        operationResultCodeResponse -> sqsService.send(
                                                        notificationTrackerSqsName.statoCartaceoName(),
                                                        createNotificationTrackerQueueDtoPaper(
                                                                cartaceoPresaInCaricoInfo,
                                                                CODE_TO_STATUS_MAP.get(
                                                                        operationResultCodeResponse.getResultCode())
                                                                //TODO
                                                                // object
                                                                // paper
                                                                ,
                                                                new PaperProgressStatusDto()))

                                                // An error occurred
                                                // during PAPER send,
                                                // start retries
                                                .retryWhen(
                                                        DEFAULT_RETRY_STRATEGY)

                                                // An error occurred
                                                // during SQS publishing
                                                // to the Notification
                                                // Tracker -> Publish to
                                                // ERRORI PAPER queue and
                                                // notify to retry
                                                // update status only
                                                // TODO: CHANGE THE PAYLOAD
                                                .onErrorResume(
                                                        throwable -> sqsService.send(
                                                                cartaceoSqsQueueName.errorName(),
                                                                cartaceoPresaInCaricoInfo))

                                ))
                // The maximum number of retries has ended
                .onErrorResume(CartaceoSendException.CartaceoMaxRetriesExceededException.class//
                        , cartaceoMaxRetriesExceeded ->

                                sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                                        RETRY.getStatusTransactionTableCompliant(),
                                                        new PaperProgressStatusDto()))

                                        // Publish to ERRORI PAPER queue
                                        .then(sqsService.send(cartaceoSqsQueueName.errorName(),
                                                cartaceoPresaInCaricoInfo))

                );

    }

    @Scheduled(cron = "${cron.value.gestione-retry-cartaceo}")
    void gestioneRetryCartaceoScheduler() {
        idSaved = null;
        sqsService.getOneMessage(cartaceoSqsQueueName.errorName(), CartaceoPresaInCaricoInfo.class)
                  .doOnNext(cartaceoPresaInCaricoInfoSqsMessageWrapper -> logIncomingMessage(cartaceoSqsQueueName.errorName(),
                                                                                             cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent()))
                  .flatMap(cartaceoPresaInCaricoInfoSqsMessageWrapper -> gestioneRetryCartaceo(cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessageContent(),
                                                                                               cartaceoPresaInCaricoInfoSqsMessageWrapper.getMessage()))
                  .map(MonoResultWrapper::new)
                  .defaultIfEmpty(new MonoResultWrapper<>(null))
                  .repeat()
                  .takeWhile(MonoResultWrapper::isNotEmpty)
                  .subscribe();
    }

    public Mono<DeleteMessageResponse> gestioneRetryCartaceo(final CartaceoPresaInCaricoInfo cartaceoPresaInCaricoInfo//
            , Message message) {
        log.info("<-- START GESTIONE RETRY CARTACEO--> Request ID : {}, Client ID : {}",
                 cartaceoPresaInCaricoInfo.getRequestIdx(),
                 cartaceoPresaInCaricoInfo.getXPagopaExtchCxId());
        String toDelete = "toDelete";
        var paperEngageRequestSrc = cartaceoPresaInCaricoInfo.getPaperEngageRequest();
        var paperEngageRequestDst = cartaceoMapper.convert(paperEngageRequestSrc);
        var requestId = cartaceoPresaInCaricoInfo.getRequestIdx();
        var clientId = cartaceoPresaInCaricoInfo.getXPagopaExtchCxId();
        Policy retryPolicies = new Policy();

        return gestoreRepositoryCall.getRichiesta(clientId, requestId)
//              check status toDelete
                                    .filter(requestDto -> !Objects.equals(requestDto.getStatusRequest(), toDelete))
//              se status toDelete throw Error
                                    .switchIfEmpty(Mono.error(new StatusToDeleteException(requestId)))
//              check Id per evitare loop
                                    .filter(requestDto -> !Objects.equals(requestDto.getRequestIdx(), idSaved))
//              se il primo step, inizializza l'attributo retry
                                    .flatMap(requestDto -> {
                                        if (requestDto.getRequestMetadata().getRetry() == null) {
                                            log.debug("Primo tentativo di Retry");
                                            RetryDto retryDto = new RetryDto();
                                            retryDto.setRetryPolicy(retryPolicies.getPolicy().get("PAPER"));
                                            retryDto.setRetryStep(BigDecimal.ZERO);
                                            retryDto.setLastRetryTimestamp(OffsetDateTime.now());
                                            requestDto.getRequestMetadata().setRetry(retryDto);
                                            PatchDto patchDto = new PatchDto();
                                            patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                                            return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);

                                        } else {
                                            var retryNumber = requestDto.getRequestMetadata().getRetry().getRetryStep();
                                            log.debug(retryNumber + " tentativo di Retry");
                                            return Mono.just(requestDto);
                                        }
                                    })
//              check retry policies
                                    .filter(requestDto -> {

                                        var dateTime1 = requestDto.getRequestMetadata().getRetry().getLastRetryTimestamp();
                                        var dateTime2 = OffsetDateTime.now();
                                        Duration duration = Duration.between(dateTime1, dateTime2);
                                        int step = requestDto.getRequestMetadata().getRetry().getRetryStep().intValueExact();
                                        long minutes = duration.toMinutes();
                                        long minutesToCheck =
                                                requestDto.getRequestMetadata().getRetry().getRetryPolicy().get(step).longValue();
                                        return minutes >= minutesToCheck;
                                    })
//              patch con orario attuale e dello step retry
                                    .flatMap(requestDto -> {
                                        requestDto.getRequestMetadata().getRetry().setLastRetryTimestamp(OffsetDateTime.now());
                                        requestDto.getRequestMetadata()
                                                  .getRetry()
                                                  .setRetryStep(requestDto.getRequestMetadata()
                                                                          .getRetry()
                                                                          .getRetryStep()
                                                                          .add(BigDecimal.ONE));
                                        PatchDto patchDto = new PatchDto();
                                        patchDto.setRetry(requestDto.getRequestMetadata().getRetry());
                                        return gestoreRepositoryCall.patchRichiesta(clientId, requestId, patchDto);
                                    })
//              Tentativo invio cartaceo
                                    .flatMap(requestDto -> {
                                        log.debug("requestDto Value: {}", requestDto.getRequestMetadata().getRetry());


                                        // Tentativo invio
                                        return paperMessageCall.putRequest(paperEngageRequestDst)

                                                               // The PAPER in sent, publish to Notification Tracker with next status ->
                                                               // SENT
                                                               .flatMap(operationResultCodeResponse ->

                                                                                sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                                                                createNotificationTrackerQueueDtoPaper(
                                                                                                        cartaceoPresaInCaricoInfo,
                                                                                                        CODE_TO_STATUS_MAP.get(
                                                                                                                operationResultCodeResponse.getResultCode()),
                                                                                                        //TODO object paper
                                                                                                        new PaperProgressStatusDto()))
                                                                                          .flatMap(sendMessageResponse -> {
                                                                                              log.debug("Il messaggio è stato gestito " +
                                                                                                        "correttamente e rimosso dalla " +
                                                                                                        "coda" + " d'errore {}",
                                                                                                        cartaceoSqsQueueName.errorName());
                                                                                              return sqsService.deleteMessageFromQueue(
                                                                                                      message,
                                                                                                      cartaceoSqsQueueName.errorName());
                                                                                          })
                                                                                          .onErrorResume(sqsPublishException -> {
                                                                                              if (idSaved == null) {
                                                                                                  idSaved = requestId;
                                                                                              }
                                                                                              if (requestDto.getRequestMetadata()
                                                                                                            .getRetry()
                                                                                                            .getRetryStep()
                                                                                                            .compareTo(BigDecimal.valueOf(3)) >
                                                                                                  0) {
                                                                                                  // operazioni per la rimozione del
                                                                                                  // messaggio
                                                                                                  log.debug(
                                                                                                          "Il messaggio è stato rimosso " +
                                                                                                          "dalla coda d'errore per " +
                                                                                                          "eccessivi tentativi: {}",
                                                                                                          cartaceoSqsQueueName.errorName());
                                                                                                  return sqsService.send(
                                                                                                                           notificationTrackerSqsName.statoCartaceoName(),
                                                                                                                           createNotificationTrackerQueueDtoPaper(
                                                                                                                                   cartaceoPresaInCaricoInfo,
                                                                                                                                   ERROR.getStatusTransactionTableCompliant(),
                                                                                                                                   //TODO
                                                                                                                                   // object paper
                                                                                                                                   new PaperProgressStatusDto()))
                                                                                                                   .flatMap(
                                                                                                                           sendMessageResponse -> sqsService.deleteMessageFromQueue(
                                                                                                                                   message,
                                                                                                                                   cartaceoSqsQueueName.errorName()));

                                                                                              }
                                                                                              return Mono.empty();
                                                                                          }));
                                    })
//              Catch errore tirato per lo stato toDelete
                                    .onErrorResume(StatusToDeleteException.class, exception -> {
                                        log.debug("Il messaggio è stato rimosso dalla coda d'errore per status toDelete: {}",
                                                  cartaceoSqsQueueName.errorName());
                                        return sqsService.send(notificationTrackerSqsName.statoCartaceoName(),
                                                               createNotificationTrackerQueueDtoPaper(cartaceoPresaInCaricoInfo,
                                                                                                      ERROR.getStatusTransactionTableCompliant(),
                                                                                                      //TODO object paper
                                                                                                      new PaperProgressStatusDto()))
                                                         .flatMap(sendMessageResponse -> sqsService.deleteMessageFromQueue(message,
                                                                                                                           cartaceoSqsQueueName.errorName()));
                                    });
    }
}
