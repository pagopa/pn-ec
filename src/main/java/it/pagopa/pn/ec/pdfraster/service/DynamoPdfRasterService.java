package it.pagopa.pn.ec.pdfraster.service;

import it.pagopa.pn.ec.pdfraster.model.dto.PdfConversionDto;
import it.pagopa.pn.ec.pdfraster.model.dto.RequestConversionDto;
import reactor.core.publisher.Mono;

public interface DynamoPdfRasterService {

    Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto request);

    Mono<PdfConversionDto> insertPdfConversion(PdfConversionDto request);

    Mono<RequestConversionDto> updateRequestConversion(RequestConversionDto requestConversionDto);




}
