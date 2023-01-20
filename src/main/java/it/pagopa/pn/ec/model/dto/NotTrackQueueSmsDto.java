package it.pagopa.pn.ec.model.dto;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static it.pagopa.pn.ec.constant.ProcessId.INVIO_SMS;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class NotTrackQueueSmsDto extends PresaInCaricoInfoDto {

    DigitalCourtesySmsRequest digitalCourtesySmsRequest;

    public NotTrackQueueSmsDto(String clientId, String currentStatus, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super(clientId, INVIO_SMS, currentStatus);
        this.digitalCourtesySmsRequest = digitalCourtesySmsRequest;
    }
}
