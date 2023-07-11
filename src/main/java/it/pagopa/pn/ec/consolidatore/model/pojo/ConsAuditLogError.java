package it.pagopa.pn.ec.consolidatore.model.pojo;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ConsAuditLogError {

    String error;
    String description;
    String requestId;

}
