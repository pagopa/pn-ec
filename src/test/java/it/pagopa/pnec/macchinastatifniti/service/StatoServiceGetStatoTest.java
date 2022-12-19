package it.pagopa.pnec.macchinastatifniti.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;


public class StatoServiceGetStatoTest {
	
	StatoService serviceImpl = new StatoService();
	
	
//	@Test
//	void getIdStatoTestOK() {
//		StatoRequest stato = new StatoRequest();
//		stato.setIdState("1");
//		serviceImpl.getIdStato(stato.getIdState());
//	}
//	
//	@Test
//	void getIdStatoTestKO() {
//		StatoRequest stato = new StatoRequest();
//		stato.setIdState(null);
//		serviceImpl.getIdStato(stato.getIdState());
//	}
	
	@Test
	void getIdStatoTestOK() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState("1");
		 Assertions.assertTrue(serviceImpl.getIdStato(stato.getIdState()));
	}
	
	@Test
	void getIdStatoTestKO() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState(null);
		 Assertions.assertFalse(serviceImpl.getIdStato(stato.getIdState()));
	}

}
