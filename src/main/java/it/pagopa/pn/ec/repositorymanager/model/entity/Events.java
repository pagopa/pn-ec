package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class Events {

	private EventsPersonal eventsPersonal;
	private EventsMetadata eventsMetadata;
}
