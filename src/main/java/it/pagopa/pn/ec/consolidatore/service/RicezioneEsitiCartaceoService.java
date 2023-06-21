package it.pagopa.pn.ec.consolidatore.service;

import it.pagopa.pn.ec.consolidatore.model.dto.RicezioneEsitiDto;
import it.pagopa.pn.ec.rest.v1.dto.ConsolidatoreIngressPaperProgressStatusEvent;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import reactor.core.publisher.Mono;

public interface RicezioneEsitiCartaceoService {
	
	Mono<RicezioneEsitiDto> verificaEsitoDaConsolidatore(
			String xPagopaExtchServiceId, ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEvent);
	
	Mono<OperationResultCodeResponse> pubblicaEsitoCodaNotificationTracker(
			String xPagopaExtchServiceId, ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEvent);

}
