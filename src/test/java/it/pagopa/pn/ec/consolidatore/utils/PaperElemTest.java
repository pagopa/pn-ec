package it.pagopa.pn.ec.consolidatore.utils;

import org.junit.jupiter.api.Test;

import static it.pagopa.pn.ec.consolidatore.utils.PaperElem.deliveryFailureCausemap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperElemTest {

    private static final String DELIVERY_FAILURE_CAUSE_M10 = "M10";
    private static final String DELIVERY_FAILURE_CAUSE_M10_DESCRIPTION = "indirizzo non leggibile";

    @Test
    void deliveryFailureCauseMapContainsM10Test() {
        assertTrue(deliveryFailureCausemap().containsKey(DELIVERY_FAILURE_CAUSE_M10));
    }

    @Test
    void deliveryFailureCauseMapM10DescriptionTest() {
        assertEquals(DELIVERY_FAILURE_CAUSE_M10_DESCRIPTION, deliveryFailureCausemap().get(DELIVERY_FAILURE_CAUSE_M10));
    }
}
