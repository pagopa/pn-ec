package it.pagopa.pn.ec.email.mapper;

import it.pagopa.pn.ec.email.model.pojo.EmailField;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;

public class EmailFieldMapper {

	public static EmailField converti(DigitalCourtesyMailRequest req) {
		EmailField ret = new EmailField();

		ret.setFrom(req.getSenderDigitalAddress());
		ret.setTo(req.getReceiverDigitalAddress());
		ret.setSubject(req.getSubjectText());

		switch (req.getMessageContentType()) {
		case PLAIN:
			ret.setTextBody(req.getMessageText());
			break;
		case HTML:
			ret.setHtmlBody(req.getMessageText());
			break;
		}

		ret.setAttachmentsUrls(req.getAttachmentsUrls());

		return ret;
	}

}