package it.pagopa.pn.library.pec.model.pojo;

import it.pagopa.pn.library.pec.pojo.PnPostacert;
import it.pagopa.pn.library.pec.model.pojo.Postacert;
import lombok.CustomLog;

import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.ERRORE_CONSEGNA;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.PREAVVISO_ERRORE_CONSEGNA;
@CustomLog

public class ArubaPostacert extends PnPostacert {
    @Override
    public String getTipo() {
        log.debug("ArubaPostacert.getTipo(): tipo={} , erroreEsteso={}", this.tipo, this.getDati().getErroreEsteso());
        if (tipo.equals(PREAVVISO_ERRORE_CONSEGNA)) {
            log.debug("ArubaPostacert.getTipo():tipo is PREAVVISO_ERRORE_CONSEGNA, checking erroreEsteso,\n tipo=" + this.tipo);
                if (this.getDati().getErroreEsteso().startsWith("5.4.1")) {
                log.debug("ArubaPostacert.getTipo():Errore esteso matches 5.4.1, changing tipo to ERRORE_CONSEGNA, errore esteso:{}", this.getDati().getErroreEsteso());
                return ERRORE_CONSEGNA;
            } else {
                log.debug("ArubaPostacert.getTipo():Errore esteso does not match 5.4.1, returning tipo, errore esteso:{}", this.getDati().getErroreEsteso());
                return this.tipo;
            }
        } else {
            log.debug("ArubaPostacert.getTipo():tipo is not PREAVVISO_ERRORE_CONSEGNA, returning tipo, tipo=" + this.tipo);
            return this.tipo;
        }
    }


    public ArubaPostacert(Postacert postacert) {
        super(postacert);
    }
}
