package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.OffsetDateTime;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class RequestMetadata {

	@Getter(onMethod=@__({@DynamoDbPartitionKey}))
	String requestId;
	// TODO: ANNOTATE WITH SECONDARY INDEX
	String messageId;
	String xPagopaExtchCxId;
	String statusRequest;
	OffsetDateTime clientRequestTimeStamp;
	OffsetDateTime requestTimestamp;
	DigitalRequestMetadata digitalRequestMetadata;
	PaperRequestMetadata paperRequestMetadata;
	List<Events> eventsList;
}
