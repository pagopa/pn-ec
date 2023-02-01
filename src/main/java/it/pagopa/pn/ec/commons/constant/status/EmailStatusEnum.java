package it.pagopa.pn.ec.commons.constant.status;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor
public enum EmailStatusEnum implements Status {

    COMPOSED("");
    final String technicalStatus;
}
