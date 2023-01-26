package it.pagopa.pn.ec.sms.model.dto;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.commons.model.pojo.PresaInCaricoInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NtStatoSmsQueueDto extends PresaInCaricoInfo {

    Status currentStatus;

    public NtStatoSmsQueueDto(PresaInCaricoInfo presaInCaricoInfo, Status currentStatus) {
        super(presaInCaricoInfo.getIdRequest(), presaInCaricoInfo.getIdClient(), presaInCaricoInfo.getProcessId());
        this.currentStatus = currentStatus;
    }
}
