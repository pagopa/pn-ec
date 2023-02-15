package it.pagopa.pn.ec.email.service;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.EcInternalEndpointHttpException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.PresaInCaricoService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.attachments.CheckAttachments;
import it.pagopa.pn.ec.email.configurationproperties.EmailSqsQueueName;
import it.pagopa.pn.ec.email.model.pojo.EmailPresaInCaricoInfo;
import it.pagopa.pn.ec.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pn.ec.rest.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.constant.ProcessId.INVIO_MAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.BATCH;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.EMAIL;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus.BOOKED;
import static java.time.OffsetDateTime.now;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EmailService extends PresaInCaricoService {

	private final SqsService sqsService;
	private final GestoreRepositoryCall gestoreRepositoryCall;
	private final AuthService authService;
	private final CheckAttachments checkAttachments;
	private final NotificationTrackerSqsName notificationTrackerSqsName;
	private final EmailSqsQueueName emailSqsQueueName;

	protected EmailService(AuthService authService, GestoreRepositoryCall gestoreRepositoryCall, SqsService sqsService,
			GestoreRepositoryCall gestoreRepositoryCall1, CheckAttachments checkAttachments,
			NotificationTrackerSqsName notificationTrackerSqsName, EmailSqsQueueName emailSqsQueueName) {
		super(authService, gestoreRepositoryCall);
		this.sqsService = sqsService;
		this.authService = authService;
		this.gestoreRepositoryCall = gestoreRepositoryCall1;
		this.checkAttachments = checkAttachments;
		this.notificationTrackerSqsName = notificationTrackerSqsName;
		this.emailSqsQueueName = emailSqsQueueName;
	}

	@Override
	protected Mono<Void> specificPresaInCarico(final PresaInCaricoInfo presaInCaricoInfo) {
		EmailPresaInCaricoInfo emailPresaInCaricoInfo = (EmailPresaInCaricoInfo) presaInCaricoInfo;

		return checkAttachments
				.checkAllegatiPresence(emailPresaInCaricoInfo.getDigitalCourtesyMailRequest().getAttachmentsUrls(),
						presaInCaricoInfo.getXPagopaExtchCxId(), false)
				.flatMap(fileDownloadResponse -> {
					var digitalNotificationRequest = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest();
					digitalNotificationRequest.setRequestId(presaInCaricoInfo.getRequestIdx());
					return insertRequestFromEmail(digitalNotificationRequest)
							.onErrorResume(throwable -> Mono.error(new EcInternalEndpointHttpException()));
				})
				.flatMap(requestDto -> sqsService.send(notificationTrackerSqsName.statoEmailName(),
						new NotificationTrackerQueueDto(presaInCaricoInfo.getRequestIdx(),
								presaInCaricoInfo.getXPagopaExtchCxId(), now(), INVIO_MAIL, null, BOOKED.getValue(),
								// TODO: Populate GeneratedMessageDto
								// Use this syntax new
								// GeneratedMessageDto().id("foo").location("bar").system("bla")
								new GeneratedMessageDto())))
				.flatMap(sendMessageResponse -> {
					DigitalCourtesyMailRequest.QosEnum qos = emailPresaInCaricoInfo.getDigitalCourtesyMailRequest()
							.getQos();
					if (qos == INTERACTIVE) {
						return sqsService.send(emailSqsQueueName.interactiveName(),
								emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
					} else if (qos == BATCH) {
						return sqsService.send(emailSqsQueueName.errorName(),
								emailPresaInCaricoInfo.getDigitalCourtesyMailRequest());
					} else {
						return Mono.empty();
					}
				}).then();
	}

	@SuppressWarnings("Duplicates")
	private Mono<RequestDto> insertRequestFromEmail(final DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
		return Mono.fromCallable(() -> {
			var requestDto = new RequestDto();
			requestDto.setRequestIdx(digitalCourtesyMailRequest.getRequestId());
			requestDto.setClientRequestTimeStamp(digitalCourtesyMailRequest.getClientRequestTimeStamp());

			var requestPersonalDto = new RequestPersonalDto();
			var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
			digitalRequestPersonalDto
					.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalCourtesyMailRequest.getQos().name()));
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

	public Flux<CourtesyMessageProgressEvent> getDigitalCourtesyMessageStatus(String requestIdx,
			String xPagopaExtchCxId) {

		return authService.clientAuth(xPagopaExtchCxId).then(gestoreRepositoryCall.getRichiesta(requestIdx))
				.onErrorResume(RestCallException.ResourceNotFoundException.class,
						e -> Mono.error(new RepositoryManagerException.RequestNotFoundException(requestIdx)))
				.map(requestDTO -> {
					var eventsListDTO = requestDTO.getRequestMetadata().getEventsList();
					List<CourtesyMessageProgressEvent> eventsList = new ArrayList<>();

					if (eventsListDTO != null && !eventsListDTO.isEmpty()) {
						for (EventsDto eventDTO : eventsListDTO) {

							var event = new CourtesyMessageProgressEvent();
							var digProgrStatus = eventDTO.getDigProgrStatus();

							event.setRequestId(requestIdx);
							event.setEventDetails(digProgrStatus.getEventDetails());
							event.setEventTimestamp(digProgrStatus.getEventTimestamp());

							// TODO: MAP INTERNAL STATUS CODE TO EXTERNAL STATUS
							event.setStatus(null);
							event.setEventCode(null);

							var generatedMessageDTO = digProgrStatus.getGeneratedMessage();
							var digitalMessageReference = new DigitalMessageReference();
							digitalMessageReference.setId(generatedMessageDTO.getId());
							digitalMessageReference.setLocation(generatedMessageDTO.getLocation());
							digitalMessageReference.setSystem(generatedMessageDTO.getSystem());

							event.setGeneratedMessage(digitalMessageReference);

							eventsList.add(event);
						}
					}
					return eventsList;
				}).flatMapIterable(courtesyMessageProgressEvents -> courtesyMessageProgressEvents);
	}

}
