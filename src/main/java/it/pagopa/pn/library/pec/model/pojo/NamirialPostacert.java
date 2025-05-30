package it.pagopa.pn.library.pec.model.pojo;


import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;

import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.ERRORE_CONSEGNA;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.PREAVVISO_ERRORE_CONSEGNA;

@CustomLog
public class NamirialPostacert extends PnPostacert {

    private final String namirialActivateLogic;


    @Override
    public String getTipo() {
        log.info("Invoking NamirialPostacert.getTipo() with flag namirialActivateLogic: {}", namirialActivateLogic);
        if ("true".equalsIgnoreCase(namirialActivateLogic) && tipo.equals(PREAVVISO_ERRORE_CONSEGNA)) {
            if (this.getDati().getErroreEsteso().trim().startsWith("5.4.1")) {
                log.debug("NamirialPostacert.getTipo():Errore esteso matches 5.4.1, changing tipo to ERRORE_CONSEGNA, errore esteso:{}", this.getDati().getErroreEsteso());
                return ERRORE_CONSEGNA;
            }
        }
        return this.tipo;
    }

    public NamirialPostacert(Postacert postacert, String namirialActivateLogic) {
        super(postacert);
        this.namirialActivateLogic = namirialActivateLogic;
    }
}
