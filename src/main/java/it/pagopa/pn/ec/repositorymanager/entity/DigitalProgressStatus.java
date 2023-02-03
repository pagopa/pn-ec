package it.pagopa.pn.ec.repositorymanager.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DigitalProgressStatus {

	private OffsetDateTime eventTimestamp;
	private String status;
	private String eventCode;
	private String eventDetails;
	private GeneratedMessage generatedMessage;
}
