package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class DigitalRequestPersonal {

	//	pec, email, sms
	String qos;
	String receiverDigitalAddress;
	String messageText;
	String senderDigitalAddress;
	String subjectText;
	List<String> attachmentsUrls;
}
