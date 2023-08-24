package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@SuperBuilder
public class RicezioneEsitiPecDto {

    String messageID;
    byte[] message;
    String receiversDomain;
    int retry;

}
