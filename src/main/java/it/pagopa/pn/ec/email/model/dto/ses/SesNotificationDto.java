package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class SesNotificationDto {

    private String notificationType;
    private SesEmailDto mail;
    private SesDeliveryDto delivery;
    private SesBounceDto bounce;
    private SesComplaintDto complaint;
    private SesRejectDto reject;
}
