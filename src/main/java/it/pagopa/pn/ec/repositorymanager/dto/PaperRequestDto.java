package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaperRequestDto {

	private String iun;
	private String requestPaid;
	private String productType;
	private List<PaperEngageRequestAttachmentsDto> attachments;
	private String printType;
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
	private Map<String, String> vas;
}
