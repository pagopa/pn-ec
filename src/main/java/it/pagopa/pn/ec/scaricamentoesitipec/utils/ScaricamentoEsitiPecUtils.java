package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.*;

public class ScaricamentoEsitiPecUtils {

    private ScaricamentoEsitiPecUtils() {
        throw new IllegalStateException("ScaricamentoEsitiPecUtils is a utility class");
    }

    public static String decodePecStatusToMachineStateStatus(String tipoPostacert) {
        return switch (tipoPostacert) {
            case PRESA_IN_CARICO -> "booked";
            case ACCETTAZIONE -> "accepted";
            case NON_ACCETTAZIONE -> "notAccepted";
            case AVVENUTA_CONSEGNA -> "delivered";
            case RILEVAZIONE_VIRUS -> "infected";
            case ERRORE_CONSEGNA -> "notDelivered";
            case PREAVVISO_ERRORE_CONSEGNA -> "deliveryWarn";
            default -> "_any_";
        };
    }
}
