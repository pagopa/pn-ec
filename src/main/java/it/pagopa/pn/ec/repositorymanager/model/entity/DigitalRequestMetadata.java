package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@DynamoDbBean
public class DigitalRequestMetadata {

    //	pec, email, sms
    String correlationId;
    String eventType;
    Map<String, String> tags;
    String channel;
    String messageContentType;
}
