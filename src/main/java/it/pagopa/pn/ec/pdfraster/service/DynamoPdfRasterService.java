package it.pagopa.pn.ec.pdfraster.service;

import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface DynamoPdfRasterService {

    Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto request);

    Mono<Map.Entry<RequestConversionDto, Boolean>> updateRequestConversion(String fileKey, Boolean converted, String fileHash, boolean isTransformationError);

}
