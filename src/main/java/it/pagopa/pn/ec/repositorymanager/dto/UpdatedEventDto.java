package it.pagopa.pn.ec.repositorymanager.dto;

import lombok.Data;

@Data
public class UpdatedEventDto {

    private DigitalProgressStatusDto digProgrStatus;
    private PaperProgressStatusDto paperProgrStatus;
}
