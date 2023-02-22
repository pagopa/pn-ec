package it.pagopa.pn.ec.pec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class sendPecMailTest {

    ServicePecImpl service = new ServicePecImpl();

    @Test
    void sendPecMailTestPositive() {
        PecObject pec = new PecObject();
        pec.setSize(10);
        Assertions.assertTrue(service.sendMail(pec), "messaggio inviato");

    }

    @Test
    void sendPecMailTestError1() {
        PecObject pec = new PecObject();
        pec.setSize(20);
        Assertions.assertFalse(service.sendMail(pec), "messaggio troppo grande");
    }

    @Test
    void sendPecMailTestError2() {
        PecObject pec = new PecObject();
        Assertions.assertFalse(service.sendMail(pec), "Impossibile mandare messaggio vuoto");
    }

}
