package it.pagopa.pn.ec.cartaceo.mapper;

import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CartaceoMapper {

    private final ObjectMapper objectMapper;

    private final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));


    public it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest convert(final it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest srcPaperEngageRequest) {
        it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest dstPaperEngageRequest = null;
        if (srcPaperEngageRequest != null) {
            OffsetDateTime srcClientRequestTimeStamp = srcPaperEngageRequest.getClientRequestTimeStamp();
            String dstClientRequestTimeStamp = null;
            srcPaperEngageRequest.setClientRequestTimeStamp(null);

            dstPaperEngageRequest = objectMapper.convertValue(srcPaperEngageRequest, it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperEngageRequest.class);

            if (srcClientRequestTimeStamp != null) {
                dstClientRequestTimeStamp = srcClientRequestTimeStamp.format(DATE_TIME_FORMATTER);
            }
            dstPaperEngageRequest.setClientRequestTimeStamp(dstClientRequestTimeStamp);
        }
        return dstPaperEngageRequest;
    }

}
