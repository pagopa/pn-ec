package it.pagopa.pnec.macchinastatifniti.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;


public class StatoServicePutUpdateStateTest {
	
	StatoService serviceImpl = new StatoService();
	
	
	@Test
	void updateStateOK() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState("B");
		 Assertions.assertTrue(serviceImpl.updateState(stato.getIdState()));
	}
	
	@Test
	void updateStateKO() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState("A");
		 Assertions.assertFalse(serviceImpl.updateState(stato.getIdState()));
	}
	

}
