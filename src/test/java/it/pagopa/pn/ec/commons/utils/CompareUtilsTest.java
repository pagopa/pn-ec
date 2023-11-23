package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static java.time.temporal.ChronoUnit.SECONDS;

class CompareUtilsTest {

    private final String nextStatus = "STATUS_2";
    private OffsetDateTime now = OffsetDateTime.now().truncatedTo(SECONDS);
    private GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();

    private DigitalProgressStatusDto lastDigitalEvent = new DigitalProgressStatusDto() {{
        setEventTimestamp(now);
        setStatus("STATUS_1");
        setGeneratedMessage(generatedMessageDto);
    }};
    private DigitalProgressStatusDto newDigitalEvent = new DigitalProgressStatusDto() {{
        setEventTimestamp(now.plusMinutes(1));
        setStatus("STATUS_2");
        setGeneratedMessage(generatedMessageDto);
    }};
    private PaperProgressStatusDto lastPaperEvent = new PaperProgressStatusDto() {{
        setStatus("STATUS_1");
        setStatusDateTime(now);
    }};
    private PaperProgressStatusDto newPaperEvent = new PaperProgressStatusDto() {{
        setStatus("STATUS_2");
        setStatusDateTime(now.plusMinutes(1));
    }};

    @Test
    void testCompareUtilsDigitalProgressStatusDtoIsNotSameEvent() {
        String nextStatus = "STATUS_2";
        boolean result = CompareUtils.isSameEvent(lastDigitalEvent, newDigitalEvent, nextStatus);
        Assertions.assertFalse(result);
    }

    @Test
    void testCompareUtilsDigitalProgressStatusDtoIsSameEvent() {
        GeneratedMessageDto generatedMessageDto = new GeneratedMessageDto();

        lastDigitalEvent.setEventTimestamp(now);
        lastDigitalEvent.setStatus("STATUS_2");
        lastDigitalEvent.setGeneratedMessage(generatedMessageDto);

        String nextStatus = "STATUS_2";

        boolean result = CompareUtils.isSameEvent(lastDigitalEvent, lastDigitalEvent, nextStatus);

        Assertions.assertTrue(result);
    }

    @Test
    void testCompareUtilsPaperProgressStatusDtoIsNotSameEvent() {
        String nextStatus = "STATUS_1";
        boolean result = CompareUtils.isSameEvent(lastPaperEvent, newPaperEvent, nextStatus);
        Assertions.assertFalse(result);
    }

    @Test
    void testCompareUtilsPaperProgressStatusDtoIsSameEvent() {
        String nextStatus = "STATUS_2";
        lastPaperEvent.setStatus("STATUS_2");
        lastPaperEvent.setStatusDateTime(now);

        boolean result = CompareUtils.isSameEvent(lastPaperEvent, lastPaperEvent, nextStatus);
        Assertions.assertTrue(result);
    }

}
