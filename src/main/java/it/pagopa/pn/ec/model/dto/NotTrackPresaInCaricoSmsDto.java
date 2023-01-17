package it.pagopa.pn.ec.model.dto;

import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesySmsRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import static it.pagopa.pn.ec.constant.ProcessId.SMS;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class NotTrackPresaInCaricoSmsDto extends PresaInCaricoInfoDto {

    DigitalCourtesySmsRequest digitalCourtesySmsRequest;

    public NotTrackPresaInCaricoSmsDto(String clientId, String currentStatus, DigitalCourtesySmsRequest digitalCourtesySmsRequest) {
        super(clientId, SMS, currentStatus);
        this.digitalCourtesySmsRequest = digitalCourtesySmsRequest;
    }
}
