package it.pagopa.pnec.pec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


class getPecMessagesCountTest {

	ServicePecImpl service= new ServicePecImpl();
	
	@Test
	void getPecMessagesCountTestPositive() {
		Integer numberOfMessages = 5;
		Assertions.assertTrue(service.getMessageCount(numberOfMessages));
	}
	


}
