package it.pagopa.pn.ec.consolidatore.model.pojo;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConsAuditLogEvent<T> {

    T request;
    List<ConsAuditLogError> errorList;

}
