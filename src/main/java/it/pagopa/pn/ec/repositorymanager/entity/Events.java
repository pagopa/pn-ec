package it.pagopa.pn.ec.repositorymanager.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class Events {

	private DigitalProgressStatus digProgrStatus;
	private PaperProgressStatus paperProgrStatus;
}
