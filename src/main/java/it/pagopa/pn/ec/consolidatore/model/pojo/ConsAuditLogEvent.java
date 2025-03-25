package it.pagopa.pn.ec.consolidatore.model.pojo;

import lombok.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConsAuditLogEvent<T> {

    T request;
    List<ConsAuditLogError> errorList;

    public ConsAuditLogEvent<T> request(T request) {
        this.request = request;
        return this;
    }

    public ConsAuditLogEvent<T> errorList(List<ConsAuditLogError> errorList) {
        this.errorList = errorList;
        return this;
    }

    @Override
    public String toString() {
        return "{" +
                "\"request\":" + request +
                ", \"errorList\":" + errorList +
                '}';
    }
}
