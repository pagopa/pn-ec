package it.pagopa.pn.ec.notificationtracker.model;


import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class NtStatoError {

    String processId;
    String clientId;
    String currStatus;
    String requestIdx;
}
