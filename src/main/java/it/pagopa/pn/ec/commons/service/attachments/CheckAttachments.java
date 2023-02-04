package it.pagopa.pn.ec.commons.service.attachments;

import it.pagopa.pn.ec.commons.rest.call.uribuilder.UriBuilderCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class CheckAttachments {

    private final UriBuilderCall uriBuilderCall;

    protected CheckAttachments(UriBuilderCall uriBuilderCall) {
        this.uriBuilderCall = uriBuilderCall;
    }

    public Mono<Void> checkAllegatiPresence(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly) {
        return Flux.fromIterable(attachmentUrls)
                .flatMap(attachmentUrl -> uriBuilderCall.getFile(attachmentUrl, xPagopaExtchCxId, metadataOnly))
                .then();
    }
}
