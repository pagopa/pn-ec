package it.pagopa.pn.ec.pec.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PecPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalNotificationRequest digitalNotificationRequest;
    String statusAfterStart;

    public PecPresaInCaricoInfo(String requestIdx, String xPagopaExtchCxId, DigitalNotificationRequest digitalNotificationRequest) {
        super(requestIdx, xPagopaExtchCxId);
        this.digitalNotificationRequest = digitalNotificationRequest;
    }
}
