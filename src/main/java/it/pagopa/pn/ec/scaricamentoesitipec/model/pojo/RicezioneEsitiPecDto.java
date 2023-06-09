package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pec.daticert.Postacert;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public class RicezioneEsitiPecDto {

    String messageID;
    byte[] daticert;
    String receiversDomain;

}
