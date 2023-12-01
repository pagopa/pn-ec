package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;

import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;

public class CompareUtils {

    private CompareUtils() {
        throw new IllegalStateException("CompareUtils is utility class");
    }

    public static boolean isSameEvent(List<EventsDto> lastEvents, DigitalProgressStatusDto newEvent, String nextStatus) {
        return lastEvents.stream().map(EventsDto::getDigProgrStatus).anyMatch(lastEvent -> lastEvent.getEventTimestamp().equals(newEvent.getEventTimestamp().truncatedTo(SECONDS))
                && lastEvent.getStatus().equals(nextStatus) && lastEvent.getGeneratedMessage() != null
                && lastEvent.getGeneratedMessage().getId().equals(newEvent.getGeneratedMessage().getId())
                && lastEvent.getGeneratedMessage().getSystem().equals(newEvent.getGeneratedMessage().getSystem()));
    }

    public static boolean isSameEvent(PaperProgressStatusDto lastEvent, PaperProgressStatusDto newEvent, String nextStatus) {
        return lastEvent.getStatus().equals(nextStatus) && lastEvent.getStatusDateTime().equals(newEvent.getStatusDateTime().truncatedTo(SECONDS));
    }

}
