package it.pagopa.pn.ec.commons.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
public class MacchinaStatiDecodeResponseDto {

	String logicStatus;
	String externalStatus;
}
