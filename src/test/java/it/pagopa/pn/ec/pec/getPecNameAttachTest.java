package it.pagopa.pn.ec.pec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class getPecNameAttachTest {

    ServicePecImpl service = new ServicePecImpl();


    @Test
    void getPecNameAttachTestPositive() {
        ParametriRicercaPec parametri = new ParametriRicercaPec();
        parametri.setMailId(2);
        parametri.setUserId(5);
        Assertions.assertTrue(service.getNameAttach(parametri), "Visualizzo name attach");
    }

}
