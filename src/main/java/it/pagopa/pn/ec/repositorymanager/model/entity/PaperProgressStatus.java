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

    String registeredLetterCode;
    String status;
    String statusCode;
    String statusDescription;
    OffsetDateTime statusDateTime;
    OffsetDateTime clientRequestTimeStamp;
    String deliveryFailureCause;
    String productType;
    String iun;
    List<PaperProgressStatusEventAttachments> attachments;
    DiscoveredAddress discoveredAddress;
}
