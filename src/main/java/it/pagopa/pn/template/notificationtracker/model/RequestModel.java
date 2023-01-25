package it.pagopa.pn.template.notificationtracker.model;

import lombok.Data;

@Data
public class RequestModel {
	
    private String processId;
    private String clientId;
    private String currStatus;
    private String nextStatus;
}
