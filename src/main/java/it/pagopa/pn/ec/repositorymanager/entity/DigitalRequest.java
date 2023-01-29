package it.pagopa.pn.ec.repositorymanager.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DigitalRequest {

//	pec, email, sms
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
