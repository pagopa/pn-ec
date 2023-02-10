package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DiscoveredAddress {

    String name;
    String nameRow2;
    String address;
    String addressRow2;
    String cap;
    String city;
    String city2;
    String pr;
    String country;
    boolean anonymous;
}
