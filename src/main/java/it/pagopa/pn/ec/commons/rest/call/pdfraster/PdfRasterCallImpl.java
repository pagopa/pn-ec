package it.pagopa.pn.ec.commons.rest.call.pdfraster;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
import it.pagopa.pn.ec.commons.exception.ClientNotAuthorizedException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic400ErrorException;
import it.pagopa.pn.ec.commons.exception.httpstatuscode.Generic500ErrorException;
import it.pagopa.pn.ec.commons.exception.ss.attachment.AttachmentNotAvailableException;
import it.pagopa.pn.ec.pdfraster.model.dto.PdfRasterResponse;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;
import static org.springframework.http.HttpStatus.*;


@Component
@CustomLog
public class PdfRasterCallImpl implements PdfRasterCall{

    private final WebClient pdfRasterWebClient;
    private final PdfRasterEndpointProperties pdfRasterEndpointProperties;

    public PdfRasterCallImpl(WebClient pdfRasterWebClient, PdfRasterEndpointProperties pdfRasterEndpointProperties){
        this.pdfRasterWebClient = pdfRasterWebClient;
        this.pdfRasterEndpointProperties = pdfRasterEndpointProperties;
    }

    @Override
    public Mono<PdfRasterResponse> convertPdf(String fileKey) throws RestCallException {
        log.logInvokingExternalService(PDF_RASTER_SERVICE,CONVERT_PDF);
        return pdfRasterWebClient.get()
                                     .uri(uriBuilder -> uriBuilder.path(pdfRasterEndpointProperties.convertPdf()).build(fileKey))
                                     .retrieve()
                                     .onStatus(NOT_FOUND::equals, clientResponse -> Mono.error(new AttachmentNotAvailableException(fileKey)))
                                     .onStatus(FORBIDDEN::equals, clientResponse -> Mono.error(new ClientNotAuthorizedException("")))
                                     .onStatus(BAD_REQUEST::equals, clientResponse -> clientResponse.createException().map(e -> new Generic400ErrorException("",e.getMessage())))
                                     .onStatus(INTERNAL_SERVER_ERROR::equals, clientResponse -> clientResponse.createException().map(e -> new Generic500ErrorException("",e.getMessage())))
                                     .bodyToMono(PdfRasterResponse.class);
    }
}
