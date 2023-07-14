package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalProgressStatus;
import it.pagopa.pn.ec.repositorymanager.model.entity.PaperProgressStatus;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;

import static java.time.temporal.ChronoUnit.SECONDS;

public class CompareUtils {

    public static boolean isSameEvent(DigitalProgressStatusDto lastEvent, DigitalProgressStatusDto newEvent, String nextStatus) {
        return lastEvent.getEventTimestamp().equals(newEvent.getEventTimestamp().truncatedTo(SECONDS))
                && lastEvent.getStatus().equals(nextStatus)
                && lastEvent.getGeneratedMessage() != null
                && lastEvent.getGeneratedMessage().equals(newEvent.getGeneratedMessage());
    }

    public static boolean isSameEvent(PaperProgressStatusDto lastEvent, PaperProgressStatusDto newEvent, String nextStatus) {
        return lastEvent.getStatus().equals(nextStatus) && lastEvent.getStatusDateTime().equals(newEvent.getStatusDateTime().truncatedTo(SECONDS));
    }

    public static boolean isSameEvent(DigitalProgressStatus lastEvent, DigitalProgressStatus newEvent) {
        return lastEvent.getEventTimestamp().equals(newEvent.getEventTimestamp().truncatedTo(SECONDS))
                && lastEvent.getStatus().equals(newEvent.getStatus())
                && lastEvent.getGeneratedMessage() != null
                && lastEvent.getGeneratedMessage().equals(newEvent.getGeneratedMessage());
    }

    public static boolean isSameEvent(PaperProgressStatus lastEvent, PaperProgressStatus newEvent) {
        return lastEvent.getStatus().equals(newEvent.getStatus()) && lastEvent.getStatusDateTime().equals(newEvent.getStatusDateTime().truncatedTo(SECONDS));
    }

}
