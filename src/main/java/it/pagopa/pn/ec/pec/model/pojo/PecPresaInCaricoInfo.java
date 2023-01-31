package it.pagopa.pn.ec.pec.model.pojo;

import it.pagopa.pn.ec.commons.constant.ProcessId;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PecPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalNotificationRequest digitalNotificationRequest;

    public PecPresaInCaricoInfo(String idRequest, String idClient, ProcessId processId, DigitalNotificationRequest digitalNotificationRequest) {
        super(idRequest, idClient, processId);
        this.digitalNotificationRequest = digitalNotificationRequest;
    }
}
