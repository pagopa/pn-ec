package it.pagopa.pn.ec.pec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class getPecMessagesCountTest {

    ServicePecImpl service = new ServicePecImpl();

    @Test
    void getPecMessagesCountTestPositive() {
        Integer numberOfMessages = 5;
        Assertions.assertTrue(service.getMessageCount(numberOfMessages));
    }


}
