package it.pagopa.pn.ec.repositorymanager.utils;

import it.pagopa.pn.ec.repositorymanager.model.entity.RequestMetadata;
import it.pagopa.pn.ec.repositorymanager.model.entity.RequestPersonal;
import it.pagopa.pn.ec.repositorymanager.model.pojo.Request;

import java.time.OffsetDateTime;

public class RequestMapper {

    private static final String SEPARATORE = "~";

    private RequestMapper() {
        throw new IllegalStateException("RequestMapper is a utility class");
    }

    public static Request createRequestFromPersonalAndMetadata(RequestPersonal requestPersonal, RequestMetadata requestMetadata) {
        String clientId = requestMetadata.getXPagopaExtchCxId();
        String currentStatus = requestMetadata.getStatusRequest();
        OffsetDateTime clientRequestTimeStamp = requestMetadata.getClientRequestTimeStamp();
        OffsetDateTime requestTimeStamp = requestMetadata.getRequestTimestamp();

        String[] parts = requestMetadata.getRequestId().split(SEPARATORE);
        String requestId = parts[1];

        return Request.builder()
                      .requestId(requestId)
                      .xPagopaExtchCxId(clientId)
                      .messageId(requestMetadata.getMessageId())
                      .statusRequest(currentStatus)
                      .clientRequestTimeStamp(clientRequestTimeStamp)
                      .requestTimeStamp(requestTimeStamp)
                      .requestPersonal(requestPersonal.getRequestId() != null ? requestPersonal : null)
                      .requestMetadata(requestMetadata)
                      .build();
    }
}
