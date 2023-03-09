package it.pagopa.pn.ec.commons.model.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PresaInCaricoInfo {

    String requestIdx;
    String xPagopaExtchCxId;
}
