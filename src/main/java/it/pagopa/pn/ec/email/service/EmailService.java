package it.pagopa.pn.ec.email.service;

import static it.pagopa.pn.ec.commons.service.SesService.DEFAULT_RETRY_STRATEGY;
import static it.pagopa.pn.ec.commons.utils.SqsUtils.logIncomingMessage;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.EMAIL;
import static java.time.OffsetDateTime.now;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SesService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.mapper.EmailFieldMapper;
import it.pagopa.pn.ec.email.model.pojo.EmailField;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestPersonalDto;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestMetadataDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestPersonalDto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Slf4j
public class EmailService extends PresaInCaricoService {

	private final SqsService sqsService;
	private final SesService sesService;
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final AttachmentServiceImpl attachmentService;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final EmailSqsQueueName emailSqsQueueName;
	private final TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;

	protected EmailService(AuthService authService//
			, GestoreRepositoryCall gestoreRepositoryCall//
			, SqsService sqsService//
			, SesService sesService//
			, GestoreRepositoryCall gestoreRepositoryCall1//
			, AttachmentServiceImpl attachmentService//
			, NotificationTrackerSqsName notificationTrackerSqsName//
			, EmailSqsQueueName emailSqsQueueName//
			, TransactionProcessConfigurationProperties transactionProcessConfigurationProperties//
	) {
		super(authService, gestoreRepositoryCall);
		this.sqsService = sqsService;
		this.sesService = sesService;
		this.gestoreRepositoryCall = gestoreRepositoryCall1;
		this.attachmentService = attachmentService;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.emailSqsQueueName = emailSqsQueueName;
		this.transactionProcessConfigurationProperties = transactionProcessConfigurationProperties;
	}

	@Override
	protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
		EmailPresaInCaricoInfo emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;
		String xPagopaExtchCxId = presaInCaricoInfo.getXPagopaExtchCxId();

		return attachmentService//
				.getAllegatiPresignedUrlOrMetadata(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getAttachmentsUrls()//
						, presaInCaricoInfo.getXPagopaExtchCxId()//
						, true)

				.flatMap(fileDownloadResponse -> {
					var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
					digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
					return insertRequestFromEmail(digitalNotificationRequest, xPagopaExtchCxId)//
							.onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
				})

				.flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoEmailName()//
						, new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx()//
								, presaInCaricoInfo.getXPagopaExtchCxId()//
								, now()//
								, transactionProcessConfigurationProperties.email()//
								, transactionProcessConfigurationProperties.emailStartStatus()//
								, "booked"//
								, null))//
				)

				.flatMap(sendMessageResponse -> {
					DigitalCourtesyMailRequest.QosEnum qos = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getQos();
					emailPresaInCaricoInfo.setStatusAfterStart("booked");
					if (qos == INTERACTIVE) {
						return sqsService.send(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);
					} else if (qos == BATCH) {
						return sqsService.send(emailSqsQueueName.batchName(), emailPresaInCaricoInfo);
					} else {
						return Mono.empty();
					}
				})

				.then();
	}

	@SuppressWarnings("Duplicates")
	private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest//
			, String xPagopaExtchCxId//
	) {
		return Mono.fromCallable(() -> {
			var requestDto = new RequestDto();
			requestDto.setRequestIdx(digitalCourtesyMailRequest.getRequestId());
			requestDto.setClientRequestTimeStamp(digitalCourtesyMailRequest.getClientRequestTimeStamp());
			requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

			var requestPersonalDto = new RequestPersonalDto();
			var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
			digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesyMailRequest.getQos().name()));
			digitalRequestPersonalDto.setReceiverDigitalAddress(digitalCourtesyMailRequest.getReceiverDigitalAddress());
			digitalRequestPersonalDto.setMessageText(digitalCourtesyMailRequest.getMessageText());
			digitalRequestPersonalDto.setSenderDigitalAddress(digitalCourtesyMailRequest.getSenderDigitalAddress());
			digitalRequestPersonalDto.setSubjectText(digitalCourtesyMailRequest.getSubjectText());
			digitalRequestPersonalDto.setAttachmentsUrls(digitalCourtesyMailRequest.getAttachmentsUrls());
			requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

			var requestMetadataDto = new RequestMetadataDto();
			var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
			digitalRequestMetadataDto.setCorrelationId(digitalCourtesyMailRequest.getCorrelationId());
			digitalRequestMetadataDto.setEventType(digitalCourtesyMailRequest.getEventType());
			digitalRequestMetadataDto.setTags(digitalCourtesyMailRequest.getTags());
			digitalRequestMetadataDto.setChannel(EMAIL);
			digitalRequestMetadataDto.setMessageContentType(DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN);
			requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

			requestDto.setRequestPersonal(requestPersonalDto);
			requestDto.setRequestMetadata(requestMetadataDto);
			return requestDto;
		}).flatMap(gestoreRepositoryCall::insertRichiesta);
	}

	@SqsListener(value = "${sqs.queue.email.interactive-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	public void lavorazioneRichiesta(final EmailPresaInCaricoInfo emailPresaInCaricoInfo//
			, final Acknowledgment acknowledgment//
	) {

		log.info("<-- START LAVORAZIONE RICHIESTA EMAIL -->");
		logIncomingMessage(emailSqsQueueName.interactiveName(), emailPresaInCaricoInfo);

		var requestId = emailPresaInCaricoInfo.getRequestIdx();
		var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
		var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
		EmailField emailField = EmailFieldMapper.converti(digitalCourtesyMailRequest);

		AtomicReference<GeneratedMessageDto> generatedMessageDto = new AtomicReference<>();

		// Try to send EMAIL
		sesService.send(emailField)

				// The EMAIL in sent, publish to Notification Tracker with next status -> SENT
				.flatMap(publishResponse -> {
					generatedMessageDto.set(new GeneratedMessageDto().id(publishResponse.messageId()).system("systemPlaceholder"));
					return sqsService.send(notificationTrackerSqsName.statoEmailName()//
							, new NotificationTrackerQueueDto(requestId//
									, clientId//
									, now()//
									, transactionProcessConfigurationProperties.email()//
									, emailPresaInCaricoInfo.getStatusAfterStart()//
									, "sent"//
					// TODO: SET eventDetails
									, ""//
									, generatedMessageDto.get()//
					));
				})

				// Delete from queue
				.doOnSuccess(result -> acknowledgment.acknowledge())

				// An error occurred during EMAIL send, start retries
				.onErrorResume(SesSendException.class//
						, sesSendException -> retryEmailSend(acknowledgment//
								, emailPresaInCaricoInfo//
								, emailPresaInCaricoInfo.getStatusAfterStart()//
								, generatedMessageDto.get()//
						))

				// An error occurred during SQS publishing to the Notification Tracker -> Publish to Errori EMAIL queue and
				// notify to retry update status only
				// TODO: CHANGE THE PAYLOAD
				.onErrorResume(SqsPublishException.class//
						, sqsPublishException -> sqsService.send(emailSqsQueueName.errorName()//
								, emailPresaInCaricoInfo//
						))//

				.subscribe();
	}

	private Mono<SendMessageResponse> retryEmailSend(final Acknowledgment acknowledgment//
			, final EmailPresaInCaricoInfo emailPresaInCaricoInfo//
			, final String currentStatus//
			, final GeneratedMessageDto generateMessageDto//
	) {

		var requestId = emailPresaInCaricoInfo.getRequestIdx();
		var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();
		var digitalCourtesyMailRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
		EmailField emailField = EmailFieldMapper.converti(digitalCourtesyMailRequest);

		// Try to send EMAIL
		return sesService.send(emailField)

				// Retry to send EMAIL
				.retryWhen(DEFAULT_RETRY_STRATEGY)

				// The EMAIL in sent, publish to Notification Tracker with next status -> SENT
				.flatMap(publishResponse -> sqsService.send(notificationTrackerSqsName.statoEmailName()//
						, new NotificationTrackerQueueDto(requestId//
								, clientId//
								, now()//
								, transactionProcessConfigurationProperties.email()//
								, currentStatus//
								, "sent"//
								// TODO: SET eventDetails
								, ""//
								, new GeneratedMessageDto().id(publishResponse.messageId()).system("systemPlaceholder")//
						)))

				// Delete from queue
				.doOnSuccess(result -> acknowledgment.acknowledge())

				// The maximum number of retries has ended
				.onErrorResume(SesSendException.SesMaxRetriesExceededException.class//
						, sesMaxRetriesExceeded -> emailRetriesExceeded(acknowledgment//
								, emailPresaInCaricoInfo//
								, generateMessageDto//
								, currentStatus//
						));

	}

	private Mono<SendMessageResponse> emailRetriesExceeded(final Acknowledgment acknowledgment//
			, final EmailPresaInCaricoInfo emailPresaInCaricoInfo//
			, final GeneratedMessageDto generatedMessageDto//
			, String currentStatus//
	) {

		var requestId = emailPresaInCaricoInfo.getRequestIdx();
		var clientId = emailPresaInCaricoInfo.getXPagopaExtchCxId();

		// Publish to Notification Tracker with next status -> RETRY
		return sqsService.send(notificationTrackerSqsName.statoEmailName()//
				, new NotificationTrackerQueueDto(requestId//
						, clientId//
						, now()//
						, transactionProcessConfigurationProperties.email()//
						, currentStatus//
						, "retry"//
						// TODO: SET eventDetails
						, ""//
						, generatedMessageDto//
				))

				// Publish to ERRORI EMAIL queue
				.then(sqsService.send(emailSqsQueueName.errorName(), emailPresaInCaricoInfo))

				// Delete from queue
				.doOnSuccess(result -> acknowledgment.acknowledge());
	}

}
