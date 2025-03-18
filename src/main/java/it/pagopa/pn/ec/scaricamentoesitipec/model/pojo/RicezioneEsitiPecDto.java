package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.s3.S3Pointer;
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
public class RicezioneEsitiPecDto extends S3Pointer {

    String messageID;
    @ToString.Exclude
    byte[] message;
    @ToString.Exclude
    String receiversDomain;
    int retry;
    String providerName;

}
