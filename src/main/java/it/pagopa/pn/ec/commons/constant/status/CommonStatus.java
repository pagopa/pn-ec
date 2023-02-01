package it.pagopa.pn.ec.commons.constant.status;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor
public enum CommonStatus implements Status {

    BOOKED(""), COMPOSITION_ERROR(""), RETRY(""), SENT("C003"), ERROR("C008");

    final String technicalStatus;
}
