package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PaperProgressStatusDto {

	private String registeredLetterCode;
	private String statusCode;
	private String statusDescription;
	private OffsetDateTime statusDateTime;
	private String deliveryFailureCause;
	private List<PaperProgressStatusEventAttachmentsDto> attachments;
	private DiscoveredAddressDto discoveredAddress;
	private OffsetDateTime clientRequestTimeStamp;
}
