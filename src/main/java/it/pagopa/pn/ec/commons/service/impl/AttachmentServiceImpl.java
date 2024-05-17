package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.ss.attachment.InvalidAttachmentSchemaException;
import it.pagopa.pn.ec.commons.rest.call.ss.file.FileCall;
import it.pagopa.pn.ec.commons.service.AttachmentService;
import it.pagopa.pn.ec.rest.v1.dto.FileDownloadResponse;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class AttachmentServiceImpl implements AttachmentService {

    private final FileCall uriBuilderCall;
    private static final String ATTACHMENT_PREFIX = "safestorage://";

    protected AttachmentServiceImpl(FileCall uriBuilderCall) {
        this.uriBuilderCall = uriBuilderCall;
    }

    @Override
    public Mono<FileDownloadResponse> getAllegatiPresignedUrlOrMetadata(String attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GET_ALLEGATI_PRESIGNED_URL_OR_METADATA, attachmentUrls);
        return Mono.just(attachmentUrls)
                   .handle((attachmentUrl, synchronousSink) -> {
                       if (!attachmentUrl.startsWith(ATTACHMENT_PREFIX)) {
                           synchronousSink.error(new InvalidAttachmentSchemaException());
                       } else {
                           synchronousSink.next(attachmentUrl);
                       }
                   })
                   .cast(String.class)
                   .flatMap(attachmentUrl -> uriBuilderCall.getFile(attachmentUrl.substring(ATTACHMENT_PREFIX.length()), xPagopaExtchCxId, metadataOnly))
                   .switchIfEmpty(Mono.just(new FileDownloadResponse()))
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_ON_LABEL, GET_ALLEGATI_PRESIGNED_URL_OR_METADATA, attachmentUrls, result));
    }

    @Override
    public Flux<FileDownloadResponse> getAllegatiPresignedUrlOrMetadata(List<String> attachmentUrls, String xPagopaExtchCxId, boolean metadataOnly) {
        log.debug(INVOKING_OPERATION_LABEL_WITH_ARGS, GET_ALLEGATI_PRESIGNED_URL_OR_METADATA, attachmentUrls);
        return Flux.fromIterable(Objects.isNull(attachmentUrls) ? List.of() : attachmentUrls)
                   .handle((attachmentUrl, synchronousSink) -> {
                       if (!attachmentUrl.startsWith(ATTACHMENT_PREFIX)) {
                           synchronousSink.error(new InvalidAttachmentSchemaException());
                       } else {
                           synchronousSink.next(attachmentUrl);
                       }
                   })
                   .cast(String.class)
                   .flatMapSequential(attachmentUrl -> uriBuilderCall.getFile(attachmentUrl.substring(ATTACHMENT_PREFIX.length()), xPagopaExtchCxId, metadataOnly))
                   .switchIfEmpty(Mono.just(new FileDownloadResponse()))
                   .doOnComplete(() -> log.info(SUCCESSFUL_OPERATION_NO_RESULT_LABEL, GET_ALLEGATI_PRESIGNED_URL_OR_METADATA));
    }
}
