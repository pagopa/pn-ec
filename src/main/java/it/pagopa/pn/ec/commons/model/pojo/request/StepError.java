package it.pagopa.pn.ec.commons.model.pojo.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import it.pagopa.pn.ec.rest.v1.dto.GeneratedMessageDto;
import it.pagopa.pn.ec.rest.v1.dto.RetryDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import it.pagopa.pn.ec.rest.v1.dto.OperationResultCodeResponse;

import java.math.BigDecimal;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Data
public class StepError {

    public enum StepErrorEnum {
        NOTIFICATION_TRACKER_STEP("NOTIFICATION_TRACKER_STEP"),
        SNS_SEND_STEP("SNS_SEND_STEP"),
        SES_SEND_STEP("SES_SEND_STEP"),
        ARUBA_SEND_MAIL_STEP("ARUBA_SEND_MAIL_STEP"),
        SET_MESSAGE_ID_STEP("SET_MESSAGE_ID_STEP"),
        PUT_REQUEST_STEP("PUT_REQUEST_STEP"),
        PDF_RASTER_STEP("PDF_RASTER_STEP"),
        END("END"),
        EXCEPTION("EXCEPTION");

        private String value;

        StepErrorEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StepErrorEnum fromValue(String value) {
            for (StepErrorEnum b : StepErrorEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    StepErrorEnum step;
    RetryDto retryDto;
    GeneratedMessageDto generatedMessageDto;
    OperationResultCodeResponse operationResultCodeResponse;
}
