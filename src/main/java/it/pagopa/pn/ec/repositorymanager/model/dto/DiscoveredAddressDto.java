package it.pagopa.pn.ec.repositorymanager.model.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class DiscoveredAddressDto {

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
