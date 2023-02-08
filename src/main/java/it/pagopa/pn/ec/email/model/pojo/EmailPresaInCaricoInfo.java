package it.pagopa.pn.ec.email.model.pojo;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalCourtesyMailRequest digitalCourtesyMailRequest;

    public EmailPresaInCaricoInfo(String requestIdx, String xPagopaExtchCxId, DigitalCourtesyMailRequest digitalCourtesyMailRequest) {
        super(requestIdx, xPagopaExtchCxId );
        this.digitalCourtesyMailRequest = digitalCourtesyMailRequest;
    }
}
