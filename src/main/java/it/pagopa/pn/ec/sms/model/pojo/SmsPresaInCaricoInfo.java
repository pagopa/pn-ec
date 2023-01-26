package it.pagopa.pn.ec.sms.model.pojo;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
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
public class SmsPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalCourtesySmsRequest digitalCourtesySmsRequest;

    public SmsPresaInCaricoInfo(String idRequest, String idClient, ProcessId processId, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super(idRequest, idClient, processId);
        this.digitalCourtesySmsRequest = digitalCourtesySmsRequest;
    }
}
