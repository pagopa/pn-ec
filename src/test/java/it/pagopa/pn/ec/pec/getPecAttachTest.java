package it.pagopa.pn.ec.pec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class getPecAttachTest {

    ServicePecImpl service = new ServicePecImpl();

    @Test
    void getPecAttachTestPositive() {
        ParametriRicercaPec parametri = new ParametriRicercaPec();
        parametri.setMailId(1);
        parametri.setUserId(1);
        parametri.setNomeAttach("attach.png");
        Assertions.assertTrue(service.getAttach(parametri), "Download dell' allegato");
    }


}
