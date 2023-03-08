package it.pagopa.pn.ec.email.mapper;

import it.pagopa.pn.ec.commons.model.pojo.email.EmailField;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;

public class EmailFieldMapper {

    private EmailFieldMapper() {
        throw new IllegalStateException("EmailFieldMapper is a utility class");
    }

    public static EmailField converti(DigitalCourtesyMailRequest req) {
        return EmailField.builder()
                         .from(req.getSenderDigitalAddress())
                         .to(req.getReceiverDigitalAddress())
                         .subject(req.getSubjectText())
                         .text(req.getMessageText())
                         .contentType(req.getMessageContentType().getValue() + "; charset=UTF-8")
                         .attachmentsUrls(req.getAttachmentsUrls())
                         .build();
    }
}