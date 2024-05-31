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
    IN_PROGRESS("inprogress"),
    DELETED("deleted"),
    INTERNAL_ERROR("internalError"),

    //  <-- PEC STATUS -->
    ACCEPTED("accepted"),
    NOT_ACCEPTED("notAccepted"),
    DELIVERED("delivered"),
    INFECTED("infected"),
    NOT_DELIVERED("notDelivered"),
    DELIVERY_WARNING("deliveryWarn"),
    NOT_PEC("nonPEC"),
    ADDRESS_ERROR("addressError");

    final String statusTransactionTableCompliant;
}
