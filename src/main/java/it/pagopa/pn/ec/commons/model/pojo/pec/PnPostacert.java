package it.pagopa.pn.ec.commons.model.pojo.pec;

import it.pec.daticert.Postacert;

public class PnPostacert extends Postacert {

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