package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
@EqualsAndHashCode
public class Events {

    DigitalProgressStatus digProgrStatus;
    PaperProgressStatus paperProgrStatus;
}
