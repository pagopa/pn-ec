package it.pagopa.pn.ec.consolidatore.model.dto;

import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RicezioneEsitiDto {
	ConsolidatoreIngressPaperProgressStatusEvent paperProgressStatusEvent;
	OperationResultCodeResponse operationResultCodeResponse;
	
	public RicezioneEsitiDto paperProgressStatusEvent(ConsolidatoreIngressPaperProgressStatusEvent paperProgressStatusEvent) {
		this.paperProgressStatusEvent = paperProgressStatusEvent;
		return this;
	}
	
	public RicezioneEsitiDto operationResultCodeResponse(OperationResultCodeResponse operationResultCodeResponse) {
		this.operationResultCodeResponse = operationResultCodeResponse;
		return this;
	}

}
