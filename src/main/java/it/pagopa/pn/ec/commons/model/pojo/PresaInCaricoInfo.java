package it.pagopa.pn.ec.commons.model.pojo;

import it.pagopa.pn.ec.commons.constant.ProcessId;
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

    String idRequest;
    String idClient;

    /**
     * Identificativo del processo richiesto dal Notification Tracker
     */
    ProcessId processId;
}
