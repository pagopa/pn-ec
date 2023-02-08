package it.pagopa.pn.ec.repositorymanager.entity;

import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DigitalProgressStatusMetadata {

	private OffsetDateTime eventTimestamp;
	private DigitalRequestStatus status;
	private String eventCode;
	private String eventDetails;
	private GeneratedMessageMetadata generatedMessage;
}
