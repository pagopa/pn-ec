package it.pagopa.pn.ec.repositorymanager.model.entity;

import it.pagopa.pn.ec.commons.model.entity.DocumentVersion;
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
public class RequestMetadata extends DocumentVersion {

	@Getter(onMethod=@__({@DynamoDbPartitionKey}))
	String requestId;
	String xPagopaExtchCxId;
	String statusRequest;
	OffsetDateTime clientRequestTimeStamp;
	OffsetDateTime requestTimestamp;
	DigitalRequestMetadata digitalRequestMetadata;
	PaperRequestMetadata paperRequestMetadata;
	List<Events> eventsList;
}
