package it.pagopa.pn.ec.commons.service.attachments;

import it.pagopa.pn.ec.commons.rest.call.uribuilder.UriBuilderCall;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class CheckAttachments {

    private final UriBuilderCall uriBuilderCall;

    protected CheckAttachments(UriBuilderCall uriBuilderCall) {
        this.uriBuilderCall = uriBuilderCall;
    }

    public Flux<FileDownloadResponse> checkAllegatiPresence(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly) {
        return Flux.fromIterable(attachmentUrls)
                .flatMap(attachmentUrl -> uriBuilderCall.getFile(attachmentUrl, xPagopaExtchCxId, metadataOnly));
    }
}
