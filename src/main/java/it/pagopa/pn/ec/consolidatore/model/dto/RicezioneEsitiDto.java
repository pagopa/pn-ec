package it.pagopa.pn.ec.consolidatore.model.dto;

import it.pagopa.pn.ec.consolidatore.model.pojo.ConsAuditLogError;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class RicezioneEsitiDto {
	ConsolidatoreIngressPaperProgressStatusEvent paperProgressStatusEvent;
	OperationResultCodeResponse operationResultCodeResponse;
	List<ConsAuditLogError> consAuditLogErrorList;
	Boolean isDuplicate;

	public RicezioneEsitiDto(ConsolidatoreIngressPaperProgressStatusEvent paperProgressStatusEvent,
							 OperationResultCodeResponse operationResultCodeResponse,
							 List<ConsAuditLogError> consAuditLogErrorList) {
		this.paperProgressStatusEvent = paperProgressStatusEvent;
		this.operationResultCodeResponse = operationResultCodeResponse;
		this.consAuditLogErrorList = consAuditLogErrorList;
	}

	public RicezioneEsitiDto paperProgressStatusEvent(ConsolidatoreIngressPaperProgressStatusEvent paperProgressStatusEvent) {
		this.paperProgressStatusEvent = paperProgressStatusEvent;
		return this;
	}
	
	public RicezioneEsitiDto operationResultCodeResponse(OperationResultCodeResponse operationResultCodeResponse) {
		this.operationResultCodeResponse = operationResultCodeResponse;
		return this;
	}

	public RicezioneEsitiDto consAuditLogErrorList(List<ConsAuditLogError> consAuditLogErrorList) {
		this.consAuditLogErrorList = consAuditLogErrorList;
		return this;
	}

	public RicezioneEsitiDto isDuplicate(Boolean isDuplicate) {
		this.isDuplicate = isDuplicate;
		return this;
	}

}
