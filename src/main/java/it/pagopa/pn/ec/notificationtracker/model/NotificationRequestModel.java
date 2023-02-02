package it.pagopa.pn.ec.notificationtracker.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"processId", "currStatus", "xpagopaExtchCxId", "nextStatus"})
public class NotificationRequestModel {
	
    private String processId;
    private String currStatus;
    private String xpagopaExtchCxId;

    private String nextStatus;

    public NotificationRequestModel(String process, String xpagopaExtchCxId, String nextStatus) {
        this.processId = process;
        this.xpagopaExtchCxId = xpagopaExtchCxId;
        this.nextStatus= nextStatus;
    }
}
