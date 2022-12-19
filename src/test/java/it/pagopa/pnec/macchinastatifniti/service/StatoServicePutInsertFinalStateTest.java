package it.pagopa.pnec.macchinastatifniti.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;
import it.pagopa.pnec.macchinastatifniti.service.StatoService;


public class StatoServicePutInsertFinalStateTest {

	
	StatoService serviceImpl = new StatoService();
	
	
	@Test
	void insertFinalStateOK() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState("C");
		 Assertions.assertTrue(serviceImpl.insertFinalState(stato.getIdState()));
	}
	
	@Test
	void insertFinalStateKO() {
		StatoRequest stato = new StatoRequest();
		stato.setIdState("B");
		 Assertions.assertFalse(serviceImpl.insertFinalState(stato.getIdState()));
	}
	
}
