package it.pagopa.pn.ec.commons.model.dto;

import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.EventsDto;
import it.pagopa.pn.ec.rest.v1.dto.PaperProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PatchDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchDtoErrorDetailsContractTest {

    private static Set<ConstraintViolation<PatchDto>> validate(PatchDto patchDto) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(patchDto);
        }
    }

    private static PatchDto digitalPatch(String errorDetails) {
        DigitalProgressStatusDto dto = new DigitalProgressStatusDto()
                .eventTimestamp(OffsetDateTime.now())
                .status("error")
                .errorDetails(errorDetails);
        return new PatchDto().event(new EventsDto().digProgrStatus(dto));
    }

    private static PatchDto paperPatch(String errorDetails) {
        PaperProgressStatusDto dto = new PaperProgressStatusDto()
                .statusDateTime(OffsetDateTime.now())
                .status("error")
                .errorDetails(errorDetails);
        return new PatchDto().event(new EventsDto().paperProgrStatus(dto));
    }

    @Test
    void digitalPatch_errorDetailsOfMaxLength_isContractValid() {
        Set<ConstraintViolation<PatchDto>> violations = validate(digitalPatch("e".repeat(1000)));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

    @Test
    void paperPatch_errorDetailsOfMaxLength_isContractValid() {
        Set<ConstraintViolation<PatchDto>> violations = validate(paperPatch("e".repeat(1000)));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

    @Test
    void digitalPatch_errorDetailsAbsent_isContractValid() {
        Set<ConstraintViolation<PatchDto>> violations = validate(digitalPatch(null));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

    @Test
    void digitalPatch_errorDetailsOverMaxLength_isNotRejectedBySchema() {
        Set<ConstraintViolation<PatchDto>> violations = validate(digitalPatch("e".repeat(5000)));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

    @Test
    void paperPatch_errorDetailsOverMaxLength_isNotRejectedBySchema() {
        Set<ConstraintViolation<PatchDto>> violations = validate(paperPatch("e".repeat(5000)));

        assertTrue(violations.isEmpty(), () -> violations.toString());
    }

}
