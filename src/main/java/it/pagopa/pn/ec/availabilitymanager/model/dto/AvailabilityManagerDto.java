package it.pagopa.pn.ec.availabilitymanager.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class AvailabilityManagerDto {
    private String version;
    private String id;
    private String detailType;
    private String source;
    private String account;
    private LocalDateTime time;
    private String region;
    private AvailabilityManagerDetailDto detail;
}
