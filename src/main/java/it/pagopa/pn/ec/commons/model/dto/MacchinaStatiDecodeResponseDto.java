package it.pagopa.pn.ec.commons.model.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class MacchinaStatiDecodeResponseDto {

	String logicStatus;
	String externalStatus;
}
