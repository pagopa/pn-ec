package it.pagopa.pn.ec.repositorymanager.dto;

import org.threeten.bp.OffsetDateTime;

public class PaperProgressStatusEventAttachmentsDto {

	private String id;
	private String documentType;
	private String uri;
	private String sha256;
	private OffsetDateTime date;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getSha256() {
		return sha256;
	}
	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}
	public OffsetDateTime getDate() {
		return date;
	}
	public void setDate(OffsetDateTime date) {
		this.date = date;
	}
	
	@Override
	public String toString() {
		return "PaperProgressStatusEventAttachmentsDto [id=" + id + ", documentType=" + documentType + ", uri=" + uri
				+ ", sha256=" + sha256 + ", date=" + date + "]";
	}
	
}
