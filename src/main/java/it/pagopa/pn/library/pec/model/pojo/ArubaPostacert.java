package it.pagopa.pn.library.pec.model.pojo;


import lombok.CustomLog;

import static it.pagopa.pn.library.pec.utils.ArubaPecUtils.ERRORE_CONSEGNA;
import static it.pagopa.pn.library.pec.utils.ArubaPecUtils.PREAVVISO_ERRORE_CONSEGNA;

@CustomLog
public class ArubaPostacert extends PnPostacert {
    @Override
    public String getTipo() {
        if (tipo.equals(PREAVVISO_ERRORE_CONSEGNA)) {
                if (this.getDati().getErroreEsteso().trim().startsWith("5.4.1")) {
                log.debug("ArubaPostacert.getTipo():Errore esteso matches 5.4.1, changing tipo to ERRORE_CONSEGNA, errore esteso:{}", this.getDati().getErroreEsteso());
                return ERRORE_CONSEGNA;
            } else {
                return this.tipo;
            }
        } else {
            return this.tipo;
        }
    }


    public ArubaPostacert(Postacert postacert) {

        super(postacert);
    }
}
