package it.pagopa.pn.ec.commons.policy;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PolicyTest {

    @Test
    void getPolicy() throws IOException {
     Policy policy = new Policy();
        List<BigDecimal> values = List.of(BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(40));
        Map<String, List<BigDecimal>> expected = new HashMap<>();

        expected.put("SMS", new ArrayList<>(values));
        expected.put("EMAIL", new ArrayList<>(values));
        expected.put("PEC", new ArrayList<>(values));
        expected.put("PAPER", new ArrayList<>(values));

        Map<String,List<BigDecimal>> map = policy.getPolicy();

        assertEquals(expected,map);
    }


}
