package it.pagopa.pn.ec.commons.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@DynamoDbBean
@ToString
public class ShedLockCounterEntity {
    @Getter(onMethod=@__({@DynamoDbPartitionKey}))
    String name;          // nome del lock
    String lockUntil;    // fino a quando il lock è valido
    String lockedAt;     // data di acquisizione del lock
    String lockedBy;      // nome dell’istanza che ha acquisito il lock
}
