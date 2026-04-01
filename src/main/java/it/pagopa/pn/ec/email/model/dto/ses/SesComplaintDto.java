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
public class SesComplaintDto {

    private List<ComplaintRecipientDto> complainedRecipients;
    private String timestamp;
}
