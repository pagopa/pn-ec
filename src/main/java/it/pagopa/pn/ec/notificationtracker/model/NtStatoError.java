package it.pagopa.pn.ec.notificationtracker.model;


import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NtStatoError {

    private String processId;
    private String clientId;
    private String currStatus;


}
