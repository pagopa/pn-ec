package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@DynamoDbBean
public class DigitalRequestPersonal {

	//	pec, email, sms
	String qos;
	@ToString.Exclude
	String receiverDigitalAddress;
	@ToString.Exclude
	String messageText;
	@ToString.Exclude
	String senderDigitalAddress;
	@ToString.Exclude
	String subjectText;
	List<String> attachmentsUrls;
}
