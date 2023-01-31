package it.pagopa.pn.ec.pec.model.dto;

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
public class NtStatoPecQueueDto extends PresaInCaricoInfo {

    Status currentStatus;

    public NtStatoPecQueueDto(PresaInCaricoInfo presaInCaricoInfo, Status currentStatus) {
        super(presaInCaricoInfo.getIdRequest(), presaInCaricoInfo.getIdClient(), presaInCaricoInfo.getProcessId());
        this.currentStatus = currentStatus;
    }
}
