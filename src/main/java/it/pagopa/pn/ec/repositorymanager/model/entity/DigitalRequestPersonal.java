package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@DynamoDbBean
public class DigitalRequestPersonal {

//	pec, email, sms
	private String qos;
	private String receiverDigitalAddress;
	private String messageText;
	private String senderDigitalAddress;
	private String subjectText;
	private List<String> attachmentsUrls;
}
