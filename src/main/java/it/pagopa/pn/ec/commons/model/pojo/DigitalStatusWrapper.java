package it.pagopa.pn.ec.commons.model.pojo;

import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatus;
import it.pagopa.pn.ec.rest.v1.dto.DigitalRequestStatusCode;
import it.pagopa.pn.ec.rest.v1.dto.ProgressEventCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class DigitalStatusWrapper {

    DigitalRequestStatus digitalRequestStatus;
    DigitalRequestStatusCode digitalRequestStatusCode;
    ProgressEventCategory progressEventCategory;
}
