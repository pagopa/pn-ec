package it.pagopa.pnec.macchinastatifniti.service;

import org.springframework.stereotype.Service;

import it.pagopa.pnec.macchinastatifniti.model.Parametri;
import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;


@Service
public class StatoService {

//	public String getIdStato(String id) {
//		StatoRequest stato = new StatoRequest();
//		stato.setIdState(id);
//		String message = "Id not found";
//		if(stato.getIdState() != null) {
//			return stato.getIdState();
//		}else {
//			return message;
//		}				
//	}
	
	public boolean getIdStato(String id ) {
		StatoRequest stato = new StatoRequest();
		stato.setIdState(id);
		if(stato.getIdState() != null) {
			return false;
		}else {
			return true;
		}
		
	}

//	public StatoRequest createStateRecord(Parametri requestParametri) {
//		StatoRequest createStato = new StatoRequest();
//
//		createStato.setIdState("A");
//		createStato.setIdMacchina(requestParametri.getIdMacchina());
//		if(createStato.getIdMacchina() != null ||createStato.getIdMacchina().equals("99") ) {
//			return createStato; 
//		}
//		else {
//			return createStato;
//		}
//		
//	
//	}
	
	public boolean createStateRecord(Parametri requestParametri) {
		StatoRequest createStato = new StatoRequest();

		createStato.setIdState("A");
		createStato.setIdMacchina(requestParametri.getIdMacchina());
		if(createStato.getIdMacchina() != null ) {
			return false; 
		}
		else {
			return true;
		}
		
	
	}

//	public String updateState(String id) {
//		StatoRequest state = new StatoRequest();
//		state.setIdState(id);
//		String message = null;
//		if(state.getIdState().equals("B")) {
//			 message = "state update";	
//		}else {
//			message = "state failed";
//		}
//		return message;
//	}
	
	

	public boolean updateState(String id) {
		StatoRequest state = new StatoRequest();
		state.setIdState(id);
		String message = null;
		if(state.getIdState().equals("B")) {
			return false;	
		}else {
			return true;	
		}
	}

//	public String insertFinalState(String id) {
//		StatoRequest state = new StatoRequest();
//		state.setIdState(id);
//		String message = null;
//		if(state.getIdState().equals("C")) {
//			 message = "state update";	
//		}else {
//			message = "state failed";
//		}
//		return message;
//	}
	
	
	public boolean insertFinalState(String id) {
		StatoRequest state = new StatoRequest();
		state.setIdState(id);
		String message = null;
		if(state.getIdState().equals("C")) {
			return false;	
		}else {
			return true;
		}
	
	}
	

}
