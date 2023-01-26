package it.pagopa.pn.ec.sms.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.RequestBaseInfo;
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
public class SmsRequestBaseInfo extends RequestBaseInfo {

    DigitalCourtesySmsRequest digitalCourtesySmsRequest;

    public SmsRequestBaseInfo(String idRequest, String idClient, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super(idRequest, idClient);
        this.digitalCourtesySmsRequest = digitalCourtesySmsRequest;
    }
}
