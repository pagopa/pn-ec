package it.pagopa.pn.ec.commons.model.pojo.request;

import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
public class StepError {

    String notificationTrackerError;
    GeneratedMessageDto generatedMessageDto;
}
