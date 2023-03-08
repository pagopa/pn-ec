package it.pagopa.pn.ec.repositorymanager.model.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
@Builder
public class Request {

    @JsonProperty("requestIdx")
    String requestId;
    @JsonProperty("xPagopaExtchCxId")
    String xPagopaExtchCxId;
    String messageId;
    String statusRequest;
    OffsetDateTime clientRequestTimeStamp;
    OffsetDateTime requestTimeStamp;
    RequestPersonal requestPersonal;
    RequestMetadata requestMetadata;
}
