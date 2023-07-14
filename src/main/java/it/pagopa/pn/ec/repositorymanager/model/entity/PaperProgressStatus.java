package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaperProgressStatus that = (PaperProgressStatus) o;
        return Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getStatusDateTime().truncatedTo(ChronoUnit.SECONDS), that.getStatusDateTime().truncatedTo(ChronoUnit.SECONDS));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStatus(), getStatusDateTime().truncatedTo(ChronoUnit.SECONDS));
    }

}
