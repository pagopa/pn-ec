package it.pagopa.pn.ec.pdfraster.service.impl;

import it.pagopa.pn.ec.pdfraster.model.dto.PdfConversionDto;
import it.pagopa.pn.ec.pdfraster.model.dto.RequestConversionDto;
import it.pagopa.pn.ec.pdfraster.service.DynamoPdfRasterService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DynamoPdfRasterServiceImpl implements DynamoPdfRasterService {
    @Override
    public Mono<RequestConversionDto> insertRequestConversion(RequestConversionDto request) {
        return null;
    }

    @Override
    public Mono<PdfConversionDto> insertPdfConversion(PdfConversionDto request) {
        return null;
    }
}
