package it.pagopa.pn.ec.commons.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PresaInCaricoInfo {

    String requestIdx;
    String xPagopaExtchCxId;
}
