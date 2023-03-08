package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@DynamoDbBean
public class DigitalProgressStatus {

	private OffsetDateTime eventTimestamp;
	private String status;
	@JsonProperty("statusCode")
	private String eventCode;
	private String eventDetails;
	private GeneratedMessage generatedMessage;
}
