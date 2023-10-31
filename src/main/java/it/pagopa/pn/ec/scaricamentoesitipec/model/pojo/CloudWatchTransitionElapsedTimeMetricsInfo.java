package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class CloudWatchTransitionElapsedTimeMetricsInfo {

    String previousStatus;
    OffsetDateTime previousEventTimestamp;
    String nextStatus;
    OffsetDateTime nextEventTimestamp;
}
