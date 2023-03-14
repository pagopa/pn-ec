package it.pagopa.pn.ec.cartaceo.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartaceoPresaInCaricoInfo  extends PresaInCaricoInfo {

    PaperEngageRequest paperEngageRequest;
    String statusAfterStart;

    public CartaceoPresaInCaricoInfo(String requestIdx, String xPagopaExtchCxId, PaperEngageRequest paperEngageRequest) {
        super(requestIdx, xPagopaExtchCxId );
        this.paperEngageRequest = paperEngageRequest;
    }

}
