package it.pagopa.pn.ec.availabilitymanager.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDetailDto;
import it.pagopa.pn.ec.availabilitymanager.model.dto.AvailabilityManagerDto;
import it.pagopa.pn.ec.cartaceo.configurationproperties.CartaceoSqsQueueName;
import it.pagopa.pn.ec.cartaceo.model.pojo.CartaceoPresaInCaricoInfo;
import it.pagopa.pn.ec.cartaceo.service.CartaceoService;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pdfraster.service.RequestConversionService;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;

@Service
@CustomLog
public class AvailabilityManagerService {

    private final RequestConversionService requestConversionService;

    private final CartaceoSqsQueueName cartaceoSqsQueueName;

    private final SqsService sqsService;

    private final CallMacchinaStati callMachinaStati;

    private final CartaceoService cartaceoService;

    private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;


    private static final String GESTORE_DISPONIBILITA_EVENT_NAME = "GESTORE DISPONIBILITA";
    private static final String EVENT_BUS_SOURCE_TRANSFORMATION_DOCUMENT = "SafeStorageTransformEvent";
    private static final String INDISPONIBILITA_EVENT_ERROR = "ERROR";
    private static final String NEXT_STATUS_TRANSFORMATION_ERROR = "transformationError";



    public AvailabilityManagerService (RequestConversionService requestConversionService, CartaceoSqsQueueName cartaceoSqsQueueName, SqsService sqsService, CallMacchinaStati callMachinaStati, CartaceoService cartaceoService, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties) {
        this.requestConversionService = requestConversionService;
        this.cartaceoSqsQueueName = cartaceoSqsQueueName;
        this.sqsService = sqsService;
        this.callMachinaStati = callMachinaStati;
        this.cartaceoService = cartaceoService;
        this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
    }

    @Value("${sqs.queue.availabilitymanager.name}")
    String availabilityManagerQueueName;

    @SqsListener(value = "${sqs.queue.availabilitymanager.name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    public void lavorazioneEsitiPecInteractive(final AvailabilityManagerDto availabilityManagerDto, Acknowledgment acknowledgment) {
        logIncomingMessage(availabilityManagerQueueName, availabilityManagerDto.toString());
        handleAvailabilityManager(availabilityManagerDto, acknowledgment).subscribe();
    }

    Mono<Void> handleAvailabilityManager(AvailabilityManagerDto availabilityManagerDto, Acknowledgment acknowledgment) {
        log.logStartingProcess(HANDLE_AVAILABILITY_MANAGER);
        return Mono.justOrEmpty(availabilityManagerDto).flatMap(dto -> {
                    AvailabilityManagerDetailDto detailDto = dto.getDetail();
                    String newFilekey = detailDto.getKey();
                    String sha256 = detailDto.getChecksum();

                    if (isSafeStorageError(dto)) {
                        log.info("Indisponibilità event found, with fileKey \"{}\" and status \"{}\": proceeding to update status and send to NotificationTracker ",dto.getDetail().getKey(), dto.getDetail().getDocumentStatus());
                        return requestConversionService.updateRequestConversion(newFilekey, false, sha256, true)
                                .map(Map.Entry::getKey).flatMap(reqConvDto -> handleTransformationError(reqConvDto, detailDto, acknowledgment))
                                .doOnSuccess(v -> log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER))
                                .doOnError(e -> log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER, false, e.getMessage()));
                    } else {
                        return requestConversionService
                                .updateRequestConversion(detailDto.getKey(), true, detailDto.getChecksum(),false)
                                .filter(Map.Entry::getValue)
                                .map(Map.Entry::getKey)
                                .filter(this::allAttachmentsConverted)
                                .map(this::buildCartaceoPresaInCaricoInfo)
                                .flatMap(info ->
                                        sqsService.send(cartaceoSqsQueueName.batchName(), info))
                                .doOnSuccess(v -> {
                                    log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER);
                                    acknowledgment.acknowledge();
                                })
                                .doOnError(e ->
                                        log.logEndingProcess(HANDLE_AVAILABILITY_MANAGER, false, e.getMessage()));
                    }
                })
                .then();
    }

    public CartaceoPresaInCaricoInfo buildCartaceoPresaInCaricoInfo(RequestConversionDto requestConversionDto) {

        List<AttachmentToConvertDto> attachments = requestConversionDto.getAttachments();
        PaperEngageRequest originalRequest = requestConversionDto.getOriginalRequest();

        originalRequest.getAttachments().forEach(paperAttachment -> {
            String oldFileKey = paperAttachment.getUri().replace("safestorage://", "");
            attachments.stream()
                    .filter(a -> oldFileKey.equals(a.getOriginalFileKey()))
                    .findFirst()
                    .ifPresent(match -> {
                        paperAttachment.setUri("safestorage://" + match.getNewFileKey());
                        paperAttachment.setSha256(match.getSha256());
                    });
        });

        CartaceoPresaInCaricoInfo info = new CartaceoPresaInCaricoInfo();
        info.setPaperEngageRequest(originalRequest);
        info.setRequestIdx(requestConversionDto.getRequestId());
        info.setXPagopaExtchCxId(requestConversionDto.getxPagopaExtchCxId());
        return info;
    }

    public boolean allAttachmentsConverted(RequestConversionDto dto) {
        return dto.getAttachments().stream().allMatch(AttachmentToConvertDto::getConverted);
    }

    /**
     * Metodo per reperire gli eventi di indisponibilità da parte di pn-ss
     */
    public boolean isSafeStorageError(AvailabilityManagerDto dto) {
        return dto != null
                && GESTORE_DISPONIBILITA_EVENT_NAME.equals(dto.getSource())
                && EVENT_BUS_SOURCE_TRANSFORMATION_DOCUMENT.equals(dto.getDetailType())
                && dto.getDetail() != null
                && INDISPONIBILITA_EVENT_ERROR.equals(dto.getDetail().getDocumentStatus());
    }

    /**
     * Invia al NT l'evento di trasformazione fallito con il nuovo stato P013
     *
     * @param requestConversionDto
     * @param detailDto
     * @param acknowledgment
     * @return
     */
    public Mono<Void> handleTransformationError(RequestConversionDto requestConversionDto,
                                                AvailabilityManagerDetailDto detailDto,
                                                Acknowledgment acknowledgment) {
        log.info(LOGGING_OPERATION_WITH_ARGS, HANDLE_AVAILABILITY_MANAGER_TRANSFORM_ERROR, requestConversionDto, detailDto);

        CartaceoPresaInCaricoInfo info = buildCartaceoPresaInCaricoInfo(requestConversionDto);
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto();
        paperProgressStatusDto.setStatus(NEXT_STATUS_TRANSFORMATION_ERROR);
        paperProgressStatusDto.setProductType(info.getPaperEngageRequest().getProductType());

        // Invio sulla coda del NotificationTracker
        log.info("Try to send message ERROR to NotificationTracker for request: {}", info);

        return cartaceoService
                .sendNotificationOnStatusQueue(info, NEXT_STATUS_TRANSFORMATION_ERROR, paperProgressStatusDto)
                .doOnSuccess(r -> {
                    log.info(SUCCESSFUL_OPERATION_LABEL, HANDLE_AVAILABILITY_MANAGER_TRANSFORM_ERROR,
                            requestConversionDto.getxPagopaExtchCxId());
                    acknowledgment.acknowledge();
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Error in handleTransformationError for requestId: {}, -> {}",
                            requestConversionDto.getxPagopaExtchCxId(), e.getMessage(), e);
                    return Mono.error(e);
                });
    }




}
