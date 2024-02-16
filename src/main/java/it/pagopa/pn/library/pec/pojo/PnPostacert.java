package it.pagopa.pn.library.pec.pojo;

import it.pagopa.pn.library.pec.model.pojo.IPostacert;
import it.pagopa.pn.library.pec.model.pojo.Postacert;

public class PnPostacert extends Postacert implements IPostacert {
	
	public PnPostacert( Postacert postacert) {
		this.dati = postacert.getDati();
		this.errore = postacert.getErrore();
		this.intestazione = postacert.getIntestazione();
		this.tipo = postacert.getTipo();
	}

    @Override
    public String getErrore() {
        String errore = super.getErrore();
        switch (errore) {
            case "nessuno" -> {
                return "";
            }
            case "no-dest" -> {
                return "no-dest";
            }
            case "no-dominio" -> {
                return "no-domain";
            }
            case "virus" -> {
                return "virus";
            }
            case "altro" -> {
                return "other";
            }
            default -> {
                return "";
            }
        }
    }
}
