package it.pagopa.pn.ec.repositorymanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString
@DynamoDbBean
public class DigitalProgressStatus {

	private OffsetDateTime timestamp;
	private String status;
	private String code;
	private String details;
	private GeneratedMessage genMess;

}
