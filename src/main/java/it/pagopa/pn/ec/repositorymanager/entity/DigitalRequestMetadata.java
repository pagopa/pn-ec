package it.pagopa.pn.ec.repositorymanager.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DigitalRequestMetadata {

//	pec, email, sms
	private String correlationId;
	private String eventType;
	private Map<String, String> tags;
	private String channel;
	private String messageContentType;
}
