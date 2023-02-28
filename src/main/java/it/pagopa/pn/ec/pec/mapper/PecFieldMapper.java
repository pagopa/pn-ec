package it.pagopa.pn.ec.pec.mapper;

import it.pagopa.pn.ec.pec.model.pojo.PecField;
import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;

public class PecFieldMapper {

    public static PecField transform(DigitalNotificationRequest pecInput) {
        PecField pecField = new PecField();

        pecField.setFrom(pecInput.getSenderDigitalAddress());
        pecField.setTo(pecInput.getReceiverDigitalAddress());
        // config set?
        pecField.setSubject(pecInput.getSubjectText());
        //html body?
        pecField.setTextBody(pecInput.getMessageText());
        pecField.setAttachmentsUrl(pecInput.getAttachmentsUrls());
        return pecField;
    }
}
