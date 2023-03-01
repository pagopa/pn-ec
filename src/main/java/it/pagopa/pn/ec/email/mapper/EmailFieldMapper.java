package it.pagopa.pn.ec.email.mapper;

import it.pagopa.pn.ec.email.model.pojo.EmailField;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;

public class EmailFieldMapper {

	public static EmailField converti(DigitalCourtesyMailRequest req) {
		EmailField ret = new EmailField();

		ret.setFrom(req.getSenderDigitalAddress());
		ret.setTo(req.getReceiverDigitalAddress());
		ret.setSubject(req.getSubjectText());
		ret.setContentObject(req.getMessageText());
		ret.setContentType(req.getMessageContentType().getValue() + "; charset=UTF-8");
		ret.setAttachmentsUrls(req.getAttachmentsUrls());

		return ret;
	}

}