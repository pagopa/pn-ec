package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@DynamoDbBean
public class DigitalProgressStatus {

    OffsetDateTime eventTimestamp;
    String status;
    @JsonProperty("statusCode")
    String eventCode;
    String eventDetails;
    GeneratedMessage generatedMessage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DigitalProgressStatus that = (DigitalProgressStatus) o;
        return Objects.equals(getEventTimestamp().truncatedTo(ChronoUnit.SECONDS), that.getEventTimestamp().truncatedTo(ChronoUnit.SECONDS)) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getGeneratedMessage(), that.getGeneratedMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEventTimestamp().truncatedTo(ChronoUnit.SECONDS), getStatus(), getGeneratedMessage());
    }
}
