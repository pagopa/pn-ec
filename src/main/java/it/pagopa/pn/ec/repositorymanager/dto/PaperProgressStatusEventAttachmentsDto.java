package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PaperProgressStatusEventAttachmentsDto {

	private String id;
	private String documentType;
	private String uri;
	private String sha256;
	private OffsetDateTime date;
}
