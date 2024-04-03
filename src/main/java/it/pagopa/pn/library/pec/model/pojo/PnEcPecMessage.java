package it.pagopa.pn.library.pec.model.pojo;

import lombok.*;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PnEcPecMessage {

    @ToString.Exclude
    private byte[] message;
    private String providerName;

    public PnEcPecMessage message(byte[] message) {
        this.message = message;
        return this;
    }

    public PnEcPecMessage providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

}
