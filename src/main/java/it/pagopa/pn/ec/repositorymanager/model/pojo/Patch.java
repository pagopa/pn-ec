package it.pagopa.pn.ec.repositorymanager.model.pojo;

import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.Retry;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Patch {

    Events event;
    Retry retry;

}
