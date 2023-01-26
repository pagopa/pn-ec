package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperEngageRequestAttachmentsDto {

	private String uri;
	private BigDecimal order;
	private String documentType;
	private String sha256;
}
