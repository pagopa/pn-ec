package it.pagopa.pn.ec.consolidatore.model.pojo;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConsAuditLogEvent {

    String requestId;
    String clientId;
    String message;

    public ConsAuditLogEvent requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public ConsAuditLogEvent clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public ConsAuditLogEvent message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        if (StringUtils.isNotBlank(requestId)) {
            return String.format("requestId: %s , clientId: %s , message: %s", requestId, clientId, message);
        } else return String.format("clientId: %s , message: %s", clientId, message);
    }
}
