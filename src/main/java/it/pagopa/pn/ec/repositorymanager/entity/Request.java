package it.pagopa.pn.ec.repositorymanager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.time.OffsetDateTime;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class Request {

	@Getter(onMethod=@__({@DynamoDbPartitionKey}))
	@JsonProperty("requestIdx")
	String requestId;
	String statusRequest;
	OffsetDateTime clientRequestTimeStamp;
	DigitalRequest digitalReq;
	PaperRequest paperReq;
	List<Events> events;
}
