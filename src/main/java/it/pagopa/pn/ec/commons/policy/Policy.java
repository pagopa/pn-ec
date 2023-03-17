package it.pagopa.pn.ec.commons.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Policy {

    File file = new File("src/main/resources/commons/retryPolicy.json");
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, List<BigDecimal>> retryPolicies;

    public Map<String, List<BigDecimal>> getPolyicy(){
    {
        try {
            retryPolicies = objectMapper.readValue(file, new TypeReference<Map<String, List<BigDecimal>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    return retryPolicies ;
    }

}
