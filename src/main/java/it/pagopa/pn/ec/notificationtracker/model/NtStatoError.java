package it.pagopa.pn.ec.notificationtracker.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@AllArgsConstructor
public class NtStatoError {

    private String processId;
    private String clientId;
    private String currStatus;
    private String requestIdx;

}
