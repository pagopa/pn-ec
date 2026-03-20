package it.pagopa.pn.ec.email.model.dto.ses;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode
@SuperBuilder
public class SesNotificationDto {

    private String notificationType;
    private SesEmailDto mail;
    private SesDeliveryDto delivery;
    private SesBounceDto bounce;
    private SesComplaintDto complaint;
    private SesRejectDto reject;
}
