package it.pagopa.pn.ec.email.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EmailPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalCourtesyMailRequest digitalCourtesyMailRequest;
    String statusAfterStart;

    public EmailPresaInCaricoInfo(String requestIdx, String xPagopaExtchCxId, DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
        super(requestIdx, xPagopaExtchCxId);
        this.digitalCourtesyMailRequest = digitalCourtesyMailRequest;
    }
}
