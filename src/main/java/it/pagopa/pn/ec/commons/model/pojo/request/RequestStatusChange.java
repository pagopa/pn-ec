package it.pagopa.pn.ec.commons.model.pojo.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class RequestStatusChange extends PresaInCaricoInfo {

    String processId;
    String currentStatus;
    String nextStatus;
}
