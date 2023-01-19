package it.pagopa.pn.ec.repositorymanager.dto;

import java.math.BigDecimal;

public class PaperEngageRequestAttachmentsDto {

	private String uri;
	private BigDecimal order;
	private String documentType;
	private String sha256;
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public BigDecimal getOrder() {
		return order;
	}
	public void setOrder(BigDecimal order) {
		this.order = order;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public String getSha256() {
		return sha256;
	}
	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}
	
	@Override
	public String toString() {
		return "PaperEngageRequestAttachmentsDto [uri=" + uri + ", order=" + order + ", documentType=" + documentType
				+ ", sha256=" + sha256 + "]";
	}
	
}
