package it.pagopa.pn.ec.cartaceo.mapper;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CartaceoMapper {

    private final ObjectMapper objectMapper;

    public org.openapitools.client.model.PaperEngageRequest convert(final it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest srcPaperEngageRequest) {
        org.openapitools.client.model.PaperEngageRequest dstPaperEngageRequest = null;
        if (srcPaperEngageRequest != null) {
            OffsetDateTime srcClientRequestTimeStamp = srcPaperEngageRequest.getClientRequestTimeStamp();
            OffsetDateTime dstClientRequestTimeStamp = null;
            srcPaperEngageRequest.setClientRequestTimeStamp(null);

            dstPaperEngageRequest = objectMapper.convertValue(srcPaperEngageRequest, org.openapitools.client.model.PaperEngageRequest.class);

            if (srcClientRequestTimeStamp != null) {
                dstClientRequestTimeStamp = OffsetDateTime.parse(srcClientRequestTimeStamp.toString());
            }
            dstPaperEngageRequest.setClientRequestTimeStamp(dstClientRequestTimeStamp);
        }
        return dstPaperEngageRequest;
    }

}
