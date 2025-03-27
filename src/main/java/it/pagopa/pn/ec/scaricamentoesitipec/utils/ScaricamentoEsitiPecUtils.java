package it.pagopa.pn.ec.scaricamentoesitipec.utils;

import static it.pagopa.pn.ec.commons.constant.Status.ACCEPTED;
import static it.pagopa.pn.ec.commons.constant.Status.DELIVERED;
import static it.pagopa.pn.ec.commons.constant.Status.DELIVERY_WARNING;
import static it.pagopa.pn.ec.commons.constant.Status.INFECTED;
import static it.pagopa.pn.ec.commons.constant.Status.NOT_ACCEPTED;
import static it.pagopa.pn.ec.commons.constant.Status.NOT_DELIVERED;
import static it.pagopa.pn.ec.commons.utils.EmailUtils.getDomainFromAddress;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.ACCETTAZIONE;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.AVVENUTA_CONSEGNA;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.ERRORE_CONSEGNA;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.NON_ACCETTAZIONE;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.PREAVVISO_ERRORE_CONSEGNA;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.RILEVAZIONE_VIRUS;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import it.pagopa.pn.ec.commons.constant.Status;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import lombok.CustomLog;

@CustomLog
public class ScaricamentoEsitiPecUtils {

    private static final int MAX_SECONDS = 60;
    private static final Random RANDOM = new Random();

    public static void sleepRandomSeconds() {
        try {
            long rs = RANDOM.nextInt(MAX_SECONDS);
            log.debug("wait {} seconds...", rs);
            TimeUnit.SECONDS.sleep(rs);
            log.debug("wait {} seconds done!", rs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ScaricamentoEsitiPecUtils() {
        throw new IllegalStateException("ScaricamentoEsitiPecUtils is a utility class");
    }

    public static final String DESTINATARIO_ESTERNO = "esterno";

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

    public static GeneratedMessageDto createGeneratedMessageByStatus(String receiverAddress, String senderAddress, String msgId, String tipoPostacert, String ssLocation) {
        var generatedMessageDto = new GeneratedMessageDto().id(msgId);
        return switch (tipoPostacert) {
        case ACCETTAZIONE, NON_ACCETTAZIONE, AVVENUTA_CONSEGNA, RILEVAZIONE_VIRUS, ERRORE_CONSEGNA -> generatedMessageDto.system(getDomainFromAddress(receiverAddress)).location(ssLocation);
        case PREAVVISO_ERRORE_CONSEGNA -> generatedMessageDto.system(getDomainFromAddress(senderAddress)).location(ssLocation);
        default -> null;
        };
    }
}
