package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode
@SuperBuilder
public class SesRejectDto {

    private String reason;
    private String additionalInfo;
}
