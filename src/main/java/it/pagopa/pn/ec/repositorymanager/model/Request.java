package it.pagopa.pn.ec.repositorymanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString
@DynamoDbBean
public class Request {

	@Getter(AccessLevel.NONE)
	private String requestId;
	private String statusRequest;
	private DigitalRequest digitalReq;
	private PaperRequest paperReq;
	private List<Events> events;
	
	@DynamoDbPartitionKey
	public String getRequestId() {
		return requestId;
	}

}
