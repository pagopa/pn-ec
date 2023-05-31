package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

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

    byte[] message;
    byte[] daticert;

}
