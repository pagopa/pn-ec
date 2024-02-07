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
        log.debug("ArubaPostacert.getTipo():tipo=" + this.tipo + ",\n erroreEsteso=" + this.getDati().getErroreEsteso());
        if (tipo.equals(PREAVVISO_ERRORE_CONSEGNA) && this.getDati().getErroreEsteso().matches("^5\\.4\\.1.*")) {
            log.debug("ArubaPostacert.getTipo():Errore esteso matches 5.4.1, changing tipo to ERRORE_CONSEGNA");
            return ERRORE_CONSEGNA;
        }
        else {return this.tipo;}
    }

    public ArubaPostacert(Postacert postacert) {
        super(postacert);
    }
}
