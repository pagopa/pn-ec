package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ss.attachment.InvalidAttachmentSchemaException;
import it.pagopa.pn.ec.commons.rest.call.uribuilder.UriBuilderCall;
import it.pagopa.pn.ec.commons.service.AttachmentService;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final UriBuilderCall uriBuilderCall;
    private static final String ATTACHMENT_PREFIX = "safestorage://";

    protected AttachmentServiceImpl(UriBuilderCall uriBuilderCall) {
        this.uriBuilderCall = uriBuilderCall;
    }

    @Override
    public Flux<FileDownloadResponse> checkAllegatiPresence(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly) {
        return Flux.fromIterable(attachmentUrls).handle((attachmentUrl, synchronousSink) -> {
            if (!attachmentUrl.startsWith(ATTACHMENT_PREFIX)) {
                synchronousSink.error(new InvalidAttachmentSchemaException());
            } else {
                synchronousSink.next(attachmentUrl);
            }
        }).flatMap(object -> {
            String attachmentUrl = (String) object;
            return uriBuilderCall.getFile(attachmentUrl.substring(ATTACHMENT_PREFIX.length()), xPagopaExtchCxId, metadataOnly);
        });
    }
}
