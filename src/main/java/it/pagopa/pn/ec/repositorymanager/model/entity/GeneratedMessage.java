package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class GeneratedMessage {

	private String system;
	private String id;
	private String location;
}
