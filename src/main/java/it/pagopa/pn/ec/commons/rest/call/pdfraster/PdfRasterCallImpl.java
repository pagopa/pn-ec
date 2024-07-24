package it.pagopa.pn.ec.commons.rest.call.pdfraster;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Component
@CustomLog
public class PdfRasterCallImpl implements PdfRasterCall{

    private final WebClient consolidatoreWebClient;
    private final PdfRasterEndpointProperties pdfRasterEndpointProperties;

    public PdfRasterCallImpl(WebClient consolidatoreWebClient, PdfRasterEndpointProperties pdfRasterEndpointProperties){
        this.consolidatoreWebClient = consolidatoreWebClient;
        this.pdfRasterEndpointProperties = pdfRasterEndpointProperties;
    }

    @Override
    public Mono<String> convertPdf(String fileKey) throws RestCallException {
        log.logInvokingExternalService(PDF_RASTER_SERVICE,CONVERT_PDF);
        return consolidatoreWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(pdfRasterEndpointProperties.convertPdf()).build(fileKey))
                                     .retrieve()
                                     .bodyToMono(String.class);
    }
}
