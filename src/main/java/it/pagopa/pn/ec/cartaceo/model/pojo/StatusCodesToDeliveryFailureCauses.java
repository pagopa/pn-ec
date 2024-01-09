package it.pagopa.pn.ec.cartaceo.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor
public class StatusCodesToDeliveryFailureCauses {
    Map<String, List<String>> statusCodeToDeliveryFailureCausesMap;

    public StatusCodesToDeliveryFailureCauses(Map<String, List<String>> deliveryFailureCauses) {
        this.statusCodeToDeliveryFailureCausesMap = deliveryFailureCauses;
    }

    public boolean isDeliveryFailureCauseInStatusCode(String statusCode, String deliveryFailureCause) {
        try {
            return (statusCodeToDeliveryFailureCausesMap.get(statusCode).contains(deliveryFailureCause)
                    || statusCodeToDeliveryFailureCausesMap.isEmpty());
        } catch (NullPointerException e) {
            return true;
        }
    }
}
