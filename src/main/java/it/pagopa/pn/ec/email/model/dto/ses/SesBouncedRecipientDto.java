package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SesBouncedRecipientDto {

    private String emailAddress;
    private String status;
    private String diagnosticCode;
}
