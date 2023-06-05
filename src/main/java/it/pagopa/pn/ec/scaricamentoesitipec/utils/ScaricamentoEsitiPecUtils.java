package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.getDomainFromAddress;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.*;

public class ScaricamentoEsitiPecUtils {

    private ScaricamentoEsitiPecUtils() {
        throw new IllegalStateException("ScaricamentoEsitiPecUtils is a utility class");
    }

    public static String DESTINATARIO_ESTERNO = "esterno";

    public static Status decodePecStatusToMachineStateStatus(String tipoPostacert) {
        return switch (tipoPostacert) {
            case ACCETTAZIONE -> ACCEPTED;
            case NON_ACCETTAZIONE -> NOT_ACCEPTED;
            case AVVENUTA_CONSEGNA -> DELIVERED;
            case RILEVAZIONE_VIRUS -> INFECTED;
            case ERRORE_CONSEGNA -> NOT_DELIVERED;
            case PREAVVISO_ERRORE_CONSEGNA -> DELIVERY_WARNING;
            default -> null;
        };
    }

    public static GeneratedMessageDto createGeneratedMessageByStatus(String receiverAddress, String senderAddress, String msgId,
                                                                     String tipoPostacert, String ssLocation) {
        var generatedMessageDto = new GeneratedMessageDto().id(msgId);
        return switch (tipoPostacert) {
            case ACCETTAZIONE, NON_ACCETTAZIONE, AVVENUTA_CONSEGNA, RILEVAZIONE_VIRUS, ERRORE_CONSEGNA ->
                    generatedMessageDto.system(getDomainFromAddress(receiverAddress)).location(ssLocation);
            case PREAVVISO_ERRORE_CONSEGNA -> generatedMessageDto.system(getDomainFromAddress(senderAddress));
            default -> null;
        };
    }
}
