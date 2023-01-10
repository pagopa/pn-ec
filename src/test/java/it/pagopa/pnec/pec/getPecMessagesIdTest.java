package it.pagopa.pnec.pec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class getPecMessagesIdTest {

	ServicePecImpl service = new ServicePecImpl();
	
	@Test
	void getPecMessagesIdTestPositive() {
		ParametriRicercaPec parametri = new ParametriRicercaPec();
		parametri.setMailId(12);
		Assertions.assertTrue(service.getPecMessagesId(parametri), "Mail visualizzata con successo");
	}
	
	@Test
	void getPecMessagesIdTestError1() {
		ParametriRicercaPec parametri = new ParametriRicercaPec();
		parametri.setMailId(-100);
		Assertions.assertFalse(service.getPecMessagesId(parametri), "Errore Mail id non valido");
	}
	
	@Test
	void getPecMessagesIdTestError2() {
		ParametriRicercaPec parametri = new ParametriRicercaPec();
		Assertions.assertFalse(service.getPecMessagesId(parametri), "Errore Null pointer");
	}

}
