package it.pagopa.pn.ec.notificationtracker.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestModel {
	
    private String processId;
    private String clientId;
    private String currStatus;
    private String nextStatus;

    public RequestModel(String process, String status, String clientId) {
        this.processId = process;
        this.currStatus = status;
        this.clientId= clientId;
    }
}
