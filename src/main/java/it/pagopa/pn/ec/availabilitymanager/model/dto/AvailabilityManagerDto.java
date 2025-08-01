package it.pagopa.pn.ec.availabilitymanager.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@ToString
public class AvailabilityManagerDto {
    private String version;
    private String id;
    @JsonProperty("detail-type")
    private String detailType;
    private String source;
    private String account;
    private LocalDateTime time;
    private String region;
    private AvailabilityManagerDetailDto detail;
}
