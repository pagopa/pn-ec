package it.pagopa.pn.ec.consolidatore.model.pojo;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConsAuditLogError {

    String error;
    String description;
    String requestId;

    public ConsAuditLogError error(String error) {
        this.error = error;
        return this;
    }

    public ConsAuditLogError description(String description) {
        this.description = description;
        return this;
    }

    public ConsAuditLogError requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                "error='" + error + '\'' +
                ", description='" + description + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
