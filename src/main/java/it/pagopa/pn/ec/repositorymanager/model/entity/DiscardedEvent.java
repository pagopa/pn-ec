package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
@EqualsAndHashCode(callSuper = false)
public class DiscardedEvent {
    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String requestId;
    @Getter(onMethod = @__({@DynamoDbSortKey}))
    String timestampRicezione;
    String dataRicezione;
    String codiceScarto;
    String jsonRicevuto;
}
