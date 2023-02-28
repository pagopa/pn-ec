package it.pagopa.pn.ec.email.mapper;

import it.pagopa.pn.ec.email.model.pojo.EmailField;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyMailRequest;

public class EmailFieldMapper {

	static final String FROM = "filippo.forcina@dgsspa.com";
	static final String TO = "filippo.forcina@dgsspa.com";
	static final String SUBJECT = "Amazon SES test (AWS SDK for Java)";
	static final String TEXT_BODY = "This email was sent through Amazon SES by using the AWS SDK for Java.";
	static final String HTML_BODY = "This email was sent through Amazon SES by using the AWS SDK for Java.";

	public static EmailField converti(DigitalCourtesyMailRequest req) {
		EmailField ret = new EmailField();

		//		ret.setFrom(req.getReceiverDigitalAddress());
		//		ret.setTo(req.getReceiverDigitalAddress());
		//		ret.setSubject(req.getSubjectText());
		//
		//		switch (req.getMessageContentType()) {
		//		case PLAIN:
		//			ret.setTextBody(req.getMessageText());
		//			break;
		//		case HTML:
		//			ret.setHtmlBody(req.getMessageText());
		//			break;
		//		}

		ret.setFrom(EmailFieldMapper.FROM);
		ret.setTo(EmailFieldMapper.TO);
		ret.setSubject(EmailFieldMapper.SUBJECT);
		ret.setTextBody(EmailFieldMapper.TEXT_BODY);
		ret.setHtmlBody(EmailFieldMapper.HTML_BODY);

		return ret;
	}

}