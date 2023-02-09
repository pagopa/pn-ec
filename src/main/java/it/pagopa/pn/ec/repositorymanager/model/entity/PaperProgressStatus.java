package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class PaperProgressStatus {

	private String registeredLetterCode;
	private String statusCode;
	private String statusDescription;
	private OffsetDateTime statusDateTime;
	private String deliveryFailureCause;
	private List<PaperProgressStatusEventAttachments> attachments;
	private DiscoveredAddress discoveredAddress;

}
