package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDbBean
@Builder
@EqualsAndHashCode
public class Events {

    DigitalProgressStatus digProgrStatus;
    PaperProgressStatus paperProgrStatus;
    String insertTimestamp;

}
