package it.pagopa.pn.ec.cartaceo.mapper;

import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CartaceoMapper {

    private final ObjectMapper objectMapper;

    public PaperEngageRequest convert(final it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest srcPaperEngageRequest) {
        PaperEngageRequest dstPaperEngageRequest = null;
        if (srcPaperEngageRequest != null) {
            //OffsetDateTime srcClientRequestTimeStamp = srcPaperEngageRequest.getClientRequestTimeStamp();
            //OffsetDateTime dstClientRequestTimeStamp = null;
            //srcPaperEngageRequest.setClientRequestTimeStamp(null);

            dstPaperEngageRequest = objectMapper.convertValue(srcPaperEngageRequest, PaperEngageRequest.class);

            //if (srcClientRequestTimeStamp != null) {
            //    dstClientRequestTimeStamp = OffsetDateTime.parse(srcClientRequestTimeStamp.toString());
            //}
            //dstPaperEngageRequest.setClientRequestTimeStamp(dstClientRequestTimeStamp);
        }
        return dstPaperEngageRequest;
    }

}
