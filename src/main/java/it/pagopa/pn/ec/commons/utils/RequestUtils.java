package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.rest.v1.dto.*;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalRequestMetadataDto.ChannelEnum.SERCQ;

public class RequestUtils {
    private static final String SEPARATORE = "~";

    private RequestUtils() {
        throw new IllegalStateException("RequestUtils is utility class");
    }


    public static String concatRequestId(String clientId, String requestId) {
        return (String.format("%s%s%s", clientId, SEPARATORE, requestId));
    }

    public static RequestDto insertRequestFromDigitalNotificationRequest(final DigitalNotificationRequest digitalNotificationRequest, String xPagopaExtchCxId, DigitalRequestMetadataDto.ChannelEnum channelEnum) {
        var requestDto = new RequestDto();
        requestDto.setRequestIdx(digitalNotificationRequest.getRequestId());
        requestDto.setClientRequestTimeStamp(digitalNotificationRequest.getClientRequestTimeStamp());
        requestDto.setxPagopaExtchCxId(xPagopaExtchCxId);

        var requestPersonalDto = new RequestPersonalDto();
        var digitalRequestPersonalDto = new DigitalRequestPersonalDto();
        digitalRequestPersonalDto.setQos(DigitalRequestPersonalDto.QosEnum.valueOf(digitalNotificationRequest.getQos().name()));
        digitalRequestPersonalDto.setReceiverDigitalAddress(digitalNotificationRequest.getReceiverDigitalAddress());
        digitalRequestPersonalDto.setMessageText(digitalNotificationRequest.getMessageText());
        digitalRequestPersonalDto.setSenderDigitalAddress(digitalNotificationRequest.getSenderDigitalAddress());
        digitalRequestPersonalDto.setSubjectText(digitalNotificationRequest.getSubjectText());
        digitalRequestPersonalDto.setAttachmentsUrls(digitalNotificationRequest.getAttachmentUrls());
        requestPersonalDto.setDigitalRequestPersonal(digitalRequestPersonalDto);

        var requestMetadataDto = new RequestMetadataDto();
        var digitalRequestMetadataDto = new DigitalRequestMetadataDto();
        digitalRequestMetadataDto.setCorrelationId(digitalNotificationRequest.getCorrelationId());
        digitalRequestMetadataDto.setEventType(digitalNotificationRequest.getEventType());
        digitalRequestMetadataDto.setTags(digitalNotificationRequest.getTags());
        digitalRequestMetadataDto.setChannel(channelEnum);
        digitalRequestMetadataDto.setMessageContentType(DigitalRequestMetadataDto.MessageContentTypeEnum.PLAIN);
        requestMetadataDto.setDigitalRequestMetadata(digitalRequestMetadataDto);

        requestDto.setRequestPersonal(requestPersonalDto);
        requestDto.setRequestMetadata(requestMetadataDto);
        return requestDto;
    }
}