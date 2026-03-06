package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SesBounceDto {

    private String bounceType;
    private String bounceSubType;
    private List<SesBouncedRecipientDto> bouncedRecipients;
    private String timestamp;
}
