package it.pagopa.pn.ec.sms.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SmsPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalCourtesySmsRequest digitalCourtesySmsRequest;
    String statusAfterStart;

    public SmsPresaInCaricoInfo(String requestIdx, String xPagopaExtchCxId, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super(requestIdx, xPagopaExtchCxId);
        this.digitalCourtesySmsRequest = digitalCourtesySmsRequest;
    }
}
