package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class DigitalProgressStatusDto {

	private OffsetDateTime timestamp;
	private String status;
	private String code;
	private String details;
	private GeneratedMessageDto genMess;
}
