package it.pagopa.pn.ec.pdfraster.service;

import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import reactor.core.publisher.Mono;

public interface DynamoPdfRasterService {

    Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto request);

    Mono<RequestConversionDto> updateRequestConversion(String fileKey, Boolean converted, String fileHash);

}
