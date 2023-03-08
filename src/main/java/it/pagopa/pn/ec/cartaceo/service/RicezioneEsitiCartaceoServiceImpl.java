package it.pagopa.pn.ec.cartaceo.service;

import org.springframework.stereotype.Service;

import it.pagopa.pn.ec.cartaceo.exception.RicezioneEsitiCartaceoException;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RicezioneEsitiCartaceoServiceImpl implements RicezioneEsitiCartaceoService {
	
	private final GestoreRepositoryCall gestoreRepositoryCall;
	
	public RicezioneEsitiCartaceoServiceImpl(GestoreRepositoryCall gestoreRepositoryCall) {
		super();
		this.gestoreRepositoryCall = gestoreRepositoryCall;
	}
	
	private void verificaPresenzaAttributiObbligatori(PaperProgressStatusEvent paperProgressStatusEvent) {
		
	}
	
	private void verificaCorrettezzaStatusCode(PaperProgressStatusEvent paperProgressStatusEvent) {
		
	}

	@Override
	public Mono<OperationResultCodeResponse> ricezioneEsitiDaConsolidatore(
			PaperProgressStatusEvent paperProgressStatusEvent) 
	{
		 Mono.just(paperProgressStatusEvent)
		     .doOnNext(event -> log.info("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : "
		     							 + "START for requestId {}",
		     							 paperProgressStatusEvent.getRequestId()))
		     .flatMap(event -> {
		    	 
		    	 if (paperProgressStatusEvent.getRequestId() == null || paperProgressStatusEvent.getRequestId().isBlank()) {
		    		 return Mono.error(new RicezioneEsitiCartaceoException("field requestId is required"));
		    	 }
		    	 
		    	 // verificare correttezza requestId
		    	 gestoreRepositoryCall.getRichiesta(paperProgressStatusEvent.getRequestId())
		    	 						.switchIfEmpty(Mono.error(new RicezioneEsitiCartaceoException("")))
		    	 						.flatMap(unused -> {
		    	 							
		    	 							return null;
		    	 						});
		    	 
		    	 return null;
		     })
		     
		     // verificare presenza attributi obbligatori
		     // verificare correttezza status code
		     // pubblicazione sulla coda del notification tracker
		     .onErrorResume(RicezioneEsitiCartaceoException.class, throwable -> {
		    	 
		    	 return null;
		     })
		     .onErrorResume(RestCallException.ResourceNotFoundException.class, throwable -> {
		    	 
		    	 return null;
		     })
		     .onErrorResume(RuntimeException.class, throwable -> {
		    	 log.error("RicezioneEsitiCartaceoServiceImpl.ricezioneEsitiDaConsolidatore() : errore generico = {}",
		    			   throwable.getMessage());
		    	 return Mono.error(new RicezioneEsitiCartaceoException(throwable.getMessage()));
		     });
		 
		 return null;
	}

}
