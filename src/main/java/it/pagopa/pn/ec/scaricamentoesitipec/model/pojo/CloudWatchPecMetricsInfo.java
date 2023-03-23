package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import it.pagopa.pn.ec.commons.constant.Status;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class CloudWatchPecMetricsInfo {

    Status previousStatus;
    OffsetDateTime previousEventTimestamp;
    Status nextStatus;
    OffsetDateTime nextEventTimestamp;
}
