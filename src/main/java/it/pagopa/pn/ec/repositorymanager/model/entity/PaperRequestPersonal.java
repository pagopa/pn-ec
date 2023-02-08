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
public class PaperRequestPersonal {

	private List<PaperEngageRequestAttachmentsPersonal> attachments;
	private String receiverName;
	private String receiverNameRow2;
	private String receiverAddress;
	private String receiverAddressRow2;
	private String receiverCap;
	private String receiverCity;
	private String receiverCity2;
	private String receiverPr;
	private String receiverCountry;
	private String receiverFiscalCode;
	private String senderName;
	private String senderAddress;
	private String senderCity;
	private String senderPr;
	private String senderDigitalAddress;
	private String arName;
	private String arAddress;
	private String arCap;
	private String arCity;
}
