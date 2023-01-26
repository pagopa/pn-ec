package it.pagopa.pn.ec.repositorymanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString
@DynamoDbBean
public class Events {

	private DigitalProgressStatus digProgrStatus;
	private PaperProgressStatus paperProgrStatus;

}
