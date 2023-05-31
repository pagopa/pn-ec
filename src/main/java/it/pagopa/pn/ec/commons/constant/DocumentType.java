package it.pagopa.pn.ec.commons.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public enum DocumentType {

    PN_EXTERNAL_LEGAL_FACTS("PN_EXTERNAL_LEGAL_FACTS");

    final String value;

}
