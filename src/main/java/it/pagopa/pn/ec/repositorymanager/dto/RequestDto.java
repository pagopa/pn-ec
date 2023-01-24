package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.util.List;

@Data
public class RequestDto {

	private String requestId;
	private DigitalRequestDto digitalReq;
	private PaperRequestDto paperReq;
	private List<EventsDto> events;
}
