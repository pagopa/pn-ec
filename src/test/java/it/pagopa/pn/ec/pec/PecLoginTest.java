package it.pagopa.pn.ec.pec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PecLoginTest {

    @Test
    void pecLoginTestPostive() {
        ServicePecImpl service = new ServicePecImpl();

        ParametriRicercaPec param = new ParametriRicercaPec();
        param.setMailboxSpaceLeft(50.4);
        Assertions.assertFalse(service.mailboxLogin(param), "Connection established");
    }

    @Test
    void pecLoginTestError1() {
        ServicePecImpl service = new ServicePecImpl();

        ParametriRicercaPec param = new ParametriRicercaPec();
        param.setMailboxSpaceLeft(-1.0);
        Assertions.assertTrue(service.mailboxLogin(param), "Connection failed");
    }
}
