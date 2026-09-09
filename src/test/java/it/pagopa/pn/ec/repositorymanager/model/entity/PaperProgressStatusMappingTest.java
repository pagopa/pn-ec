package it.pagopa.pn.ec.repositorymanager.model.entity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.ec.commons.utils.RestUtils;
import it.pagopa.pn.ec.rest.v1.dto.AttachmentsProgressEventDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

class PaperProgressStatusMappingTest {

    private final RestUtils restUtils = new RestUtils(
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

    private PaperProgressStatusDto baseDto() {
        return new PaperProgressStatusDto()
                .status("SENT")
                .statusCode("RECRN002A")
                .statusDateTime(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .productType("AR")
                .iun("ABCD-HILM-YKWX-202202-1")
                .registeredLetterCode("123456789abc")
                .courier("POSTE");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void isDuplicateSurvivesDtoToEntity(Boolean isDuplicate) {
        PaperProgressStatusDto dto = baseDto().isDuplicate(isDuplicate);

        PaperProgressStatus entity = restUtils.startCreateRequest(dto, PaperProgressStatus.class);

        Assertions.assertEquals(isDuplicate, entity.getIsDuplicate());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {true, false})
    void isDuplicateSurvivesRoundTrip(Boolean isDuplicate) {
        PaperProgressStatusDto dto = baseDto().isDuplicate(isDuplicate);

        PaperProgressStatus entity = restUtils.startCreateRequest(dto, PaperProgressStatus.class);
        PaperProgressStatusDto back = restUtils.entityToDto(entity, PaperProgressStatusDto.class);

        Assertions.assertEquals(isDuplicate, back.getIsDuplicate());
    }

    private PaperProgressStatusDto dtoWithAttachment(String sourceType, String originType) {
        return baseDto().attachments(List.of(new AttachmentsProgressEventDto()
                .id("0")
                .documentType("23L")
                .uri("safestorage://PN_EXTERNAL_LEGAL_FACTS-abcd")
                .sha256("sha256")
                .date(OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .sourceType(sourceType)
                .originType(originType)));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {"SCANNED,ORIGINAL", "DIGITAL,DUPLICATED", "NULL,NULL"})
    void sourceTypeAndOriginTypeSurviveDtoToEntity(String sourceType, String originType) {
        PaperProgressStatusDto dto = dtoWithAttachment(sourceType, originType);

        PaperProgressStatus entity = restUtils.startCreateRequest(dto, PaperProgressStatus.class);

        PaperProgressStatusEventAttachments attachment = entity.getAttachments().get(0);
        Assertions.assertEquals(sourceType, attachment.getSourceType());
        Assertions.assertEquals(originType, attachment.getOriginType());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {"SCANNED,ORIGINAL", "DIGITAL,DUPLICATED", "NULL,NULL"})
    void sourceTypeAndOriginTypeSurviveRoundTrip(String sourceType, String originType) {
        PaperProgressStatusDto dto = dtoWithAttachment(sourceType, originType);

        PaperProgressStatus entity = restUtils.startCreateRequest(dto, PaperProgressStatus.class);
        PaperProgressStatusDto back = restUtils.entityToDto(entity, PaperProgressStatusDto.class);

        AttachmentsProgressEventDto attachment = back.getAttachments().get(0);
        Assertions.assertEquals(sourceType, attachment.getSourceType());
        Assertions.assertEquals(originType, attachment.getOriginType());
    }
}
