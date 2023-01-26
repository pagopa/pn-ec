package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class DigitalRequestDto {

	private String correlationId;
	private String eventType;
	private String qos;
	private List<String> tags;
	private OffsetDateTime clientRequestTimeStamp;
	private String receiverDigitalAddress;
	private String messageText;
	private String senderDigitalAddress;
	private String channel;
	private String subjectText;
	private String messageContentType;
	private List<String> attachmentsUrls;

}
