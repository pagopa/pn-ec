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
public class SesComplaintDto {

    private List<ComplaintRecipientDto> complainedRecipients;
    private String timestamp;
}
