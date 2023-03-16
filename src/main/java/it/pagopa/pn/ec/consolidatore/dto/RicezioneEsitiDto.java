package it.pagopa.pn.ec.consolidatore.dto;

import java.io.Serializable;

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
public class RicezioneEsitiDto implements Serializable {

	private static final long serialVersionUID = -2206916494825569451L;
	
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
