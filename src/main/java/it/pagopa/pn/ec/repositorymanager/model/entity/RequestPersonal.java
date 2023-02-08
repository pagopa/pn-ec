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
public class RequestPersonal {

	@Getter(onMethod=@__({@DynamoDbPartitionKey}))
	String requestId;
	OffsetDateTime clientRequestTimeStamp;
	OffsetDateTime requestTimestamp;
	DigitalRequestPersonal digitalRequestPersonal;
	PaperRequestPersonal paperRequestPersonal;
	List<EventsPersonal> eventsPersonalList;
}
