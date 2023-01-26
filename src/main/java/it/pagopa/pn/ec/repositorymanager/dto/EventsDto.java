package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

@Data
public class EventsDto {

    private DigitalProgressStatusDto digProgrStatus;
    private PaperProgressStatusDto paperProgrStatus;
}
