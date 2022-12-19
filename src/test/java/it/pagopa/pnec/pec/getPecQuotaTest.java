package it.pagopa.pnec.pec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


class getPecQuotaTest {
	
	ServicePecImpl service= new ServicePecImpl();

	@Test
	void getPecQuotaTestPositive() {
		ParametriRicercaPec parametri = new ParametriRicercaPec();
		parametri.setMailboxSpaceLeft(125.50);
		Assertions.assertTrue(service.getQuota(parametri), "Visualizzo spazio casella postale");
	}
}
