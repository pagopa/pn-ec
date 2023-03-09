package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class DigitalProgressStatus {

	OffsetDateTime eventTimestamp;
	String status;
	@JsonProperty("statusCode")
	String eventCode;
	String eventDetails;
	GeneratedMessage generatedMessage;
}
