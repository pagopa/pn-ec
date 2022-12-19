package it.pagopa.pnec.macchinastatifniti.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import it.pagopa.pnec.macchinastatifniti.model.Parametri;
import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;


public class StatoServicePostCreateNewStateTest {
	
	StatoService serviceImpl = new StatoService();
	
	@Test
	void createStateRecordTestOK() {
		Parametri parametri = new Parametri();
		StatoRequest createStato = new StatoRequest();
		parametri.setIdStato("A");
		parametri.setIdMacchina("1");
		 Assertions.assertTrue(serviceImpl.createStateRecord(parametri));
	}
	
	@Test
	void createStateRecordTestKO() {
		Parametri parametri = new Parametri();
		parametri.setIdStato("B");
		parametri.setIdMacchina(null);
;
		 Assertions.assertFalse(serviceImpl.createStateRecord(parametri));
	}
	

}
