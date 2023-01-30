package it.pagopa.pn.ec.repositorymanager.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
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
	private String requestId;
	private String statusRequest;
	private DigitalRequest digitalReq;
	private PaperRequest paperReq;
	private List<Events> events;
}
