package it.pagopa.pn.ec.repositorymanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString
@DynamoDbBean
public class DiscoveredAddress {

    private String name;
    private String nameRow2;
    private String address;
    private String addressRow2;
    private String cap;
    private String city;
    private String city2;
    private String pr;
    private String country;
}
