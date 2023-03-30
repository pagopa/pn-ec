package it.pagopa.pn.ec.commons.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public enum Status {

    //  <-- COMMON STATUS -->
    BOOKED("booked"),
    SENT("sent"),
    RETRY("retry"),
    ERROR("error"),

    DELETED("deleted"),

    //  <-- ONLY EMAIL AND PEC -->
    COMPOSITION_ERROR("compError"),

    //  <-- PEC STATUS -->
    ACCEPTED("accepted"),
    NOT_ACCEPTED("notAccepted"),
    DELIVERED("delivered"),
    INFECTED("infected"),
    NOT_DELIVERED("notDelivered"),
    DELIVERY_WARNING("deliveryWarn");

    final String statusTransactionTableCompliant;
}
