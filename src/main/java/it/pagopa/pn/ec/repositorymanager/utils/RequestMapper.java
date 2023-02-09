package it.pagopa.pn.ec.repositorymanager.utils;

import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;

import java.time.OffsetDateTime;

public class RequestMapper {

    private RequestMapper() {
        throw new IllegalStateException("Request Mapper is a utility class");
    }

    public static Request createRequestFromPersonalAndMetadata(String requestId, RequestPersonal requestPersonal,
                                                               RequestMetadata requestMetadata) {
        String currentStatus = requestMetadata.getStatusRequest();
        OffsetDateTime clientRequestTimeStamp = requestMetadata.getClientRequestTimeStamp();
        OffsetDateTime requestTimeStamp = requestMetadata.getRequestTimestamp();
        return Request.builder()
                .requestId(requestId)
                .statusRequest(currentStatus)
                .clientRequestTimeStamp(clientRequestTimeStamp)
                .requestTimeStamp(requestTimeStamp)
                .requestPersonal(requestPersonal)
                .requestMetadata(requestMetadata)
                .build();
    }
}
