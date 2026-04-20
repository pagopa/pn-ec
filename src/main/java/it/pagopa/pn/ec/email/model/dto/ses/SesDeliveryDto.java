package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode
@SuperBuilder
public class SesDeliveryDto {

    private String timestamp;
    private String smtpResponse;
    private List<String> recipients;
    private Long processingTimeMillis;
    private String reportingMTA;
}
