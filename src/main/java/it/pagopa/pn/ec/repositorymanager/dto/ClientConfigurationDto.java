package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

@Data
public class ClientConfigurationDto {

	private String cxId;
	private String sqsArn;
	private String sqsName;
	private String pecReplyTo;
	private String mailReplyTo;
	private SenderPhysicalAddressDto senderPhysicalAddress;

}
