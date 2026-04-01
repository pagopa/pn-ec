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
public class SesBounceDto {

    private String bounceType;
    private String bounceSubType;
    private List<SesBouncedRecipientDto> bouncedRecipients;
    private String timestamp;
}
