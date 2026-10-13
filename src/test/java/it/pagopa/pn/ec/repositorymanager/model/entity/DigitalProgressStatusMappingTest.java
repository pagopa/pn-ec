package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

class DigitalProgressStatusMappingTest {

    private final RestUtils restUtils = new RestUtils(
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

    private DigitalProgressStatusDto baseDto() {
        return new DigitalProgressStatusDto()
                .eventTimestamp(OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS))
                .status("retry")
                .statusCode("C000")
                .eventDetails("details")
                .generatedMessage(new GeneratedMessageDto().id("messageId").system("system"));
    }

    @Test
    void errorDetailsSurvivesRoundTrip() {
        String errorDetails = "Exception in 'lavorazioneRichiestaEmail()' - java.lang.RuntimeException: failure, failure";
        DigitalProgressStatusDto dto = baseDto().errorDetails(errorDetails);

        DigitalProgressStatus entity = restUtils.startCreateRequest(dto, DigitalProgressStatus.class);
        DigitalProgressStatusDto back = restUtils.entityToDto(entity, DigitalProgressStatusDto.class);

        Assertions.assertEquals(errorDetails, back.getErrorDetails());
    }

    @Test
    void errorDetailsOfMaxLengthSurvivesRoundTrip() {
        String errorDetails = "e".repeat(1000);
        DigitalProgressStatusDto dto = baseDto().errorDetails(errorDetails);

        DigitalProgressStatus entity = restUtils.startCreateRequest(dto, DigitalProgressStatus.class);
        DigitalProgressStatusDto back = restUtils.entityToDto(entity, DigitalProgressStatusDto.class);

        Assertions.assertEquals(errorDetails, back.getErrorDetails());
    }

    @Test
    void errorDetailsAbsentRemainsNullAfterRoundTrip() {
        DigitalProgressStatusDto dto = baseDto();

        DigitalProgressStatus entity = restUtils.startCreateRequest(dto, DigitalProgressStatus.class);
        DigitalProgressStatusDto back = restUtils.entityToDto(entity, DigitalProgressStatusDto.class);

        Assertions.assertNull(back.getErrorDetails());
    }

    @Test
    void errorDetailsDoesNotAlterOtherFieldsInRoundTrip() {
        DigitalProgressStatusDto dto = baseDto().errorDetails("failure");

        DigitalProgressStatus entity = restUtils.startCreateRequest(dto, DigitalProgressStatus.class);
        DigitalProgressStatusDto back = restUtils.entityToDto(entity, DigitalProgressStatusDto.class);

        Assertions.assertEquals(dto.getStatus(), back.getStatus());
        Assertions.assertEquals(dto.getStatusCode(), back.getStatusCode());
        Assertions.assertEquals(dto.getEventDetails(), back.getEventDetails());
        Assertions.assertEquals(dto.getEventTimestamp(), back.getEventTimestamp());
        Assertions.assertEquals(dto.getGeneratedMessage().getId(), back.getGeneratedMessage().getId());
    }

    @Test
    void equalsIgnoresErrorDetails() {
        DigitalProgressStatusDto withDetails = baseDto().errorDetails("failure");
        DigitalProgressStatusDto withoutDetails = baseDto().eventTimestamp(withDetails.getEventTimestamp());

        DigitalProgressStatus first = restUtils.startCreateRequest(withDetails, DigitalProgressStatus.class);
        DigitalProgressStatus second = restUtils.startCreateRequest(withoutDetails, DigitalProgressStatus.class);

        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
    }

}
