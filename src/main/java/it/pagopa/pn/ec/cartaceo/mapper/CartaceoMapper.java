package it.pagopa.pn.ec.cartaceo.mapper;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartaceoMapper {

    private final ObjectMapper objectMapper;

    public org.openapitools.client.model.PaperEngageRequest convert(final it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest srcPaperEngageRequest) {
        org.openapitools.client.model.PaperEngageRequest dstPaperEngageRequest = null;
        if (srcPaperEngageRequest != null) {
            java.time.OffsetDateTime srcClientRequestTimeStamp = srcPaperEngageRequest.getClientRequestTimeStamp();
            org.threeten.bp.OffsetDateTime dstClientRequestTimeStamp = null;
            srcPaperEngageRequest.setClientRequestTimeStamp(null);

            dstPaperEngageRequest = objectMapper.convertValue(srcPaperEngageRequest, org.openapitools.client.model.PaperEngageRequest.class);

            if (srcClientRequestTimeStamp != null) {
                dstClientRequestTimeStamp = org.threeten.bp.OffsetDateTime.parse(srcClientRequestTimeStamp.toString());
            }
            dstPaperEngageRequest.setClientRequestTimeStamp(dstClientRequestTimeStamp);
        }
        return dstPaperEngageRequest;
    }

}
