package it.pagopa.pn.ec.pdfraster.service.impl;

import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import it.pagopa.pn.ec.rest.v1.dto.PdfConversionDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestConversionDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DynamoPdfRasterServiceImpl implements DynamoPdfRasterService {
    @Override
    public Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto request) {
        return null;
    }

    @Override
    public Mono<RequestConversionDto> updateRequestConversion(RequestConversionDto requestConversionDto) {
        return null;
    }

    @Override
    public Mono<PdfConversionDto> insertPdfConversion(PdfConversionDto request) {
        return null;
    }



}
