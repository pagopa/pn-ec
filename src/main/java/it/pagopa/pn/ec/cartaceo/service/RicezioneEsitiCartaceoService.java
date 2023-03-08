package it.pagopa.pn.ec.cartaceo.service;

import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import reactor.core.publisher.Mono;

public interface RicezioneEsitiCartaceoService {
	
	Mono<OperationResultCodeResponse> ricezioneEsitiDaConsolidatore(PaperProgressStatusEvent paperProgressStatusEvent);

}
