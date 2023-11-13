package it.pagopa.pn.ec.commons.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.PolicyFileNotFoundException;
import lombok.CustomLog;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@CustomLog
public class Policy {


    public Map<String, List<BigDecimal>> getPolicy() {
        try (
            InputStream inputStream = getClass().getResourceAsStream("/commons/retryPolicy.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, List<BigDecimal>>>() {});
        } catch (IOException e) {
            // Gestione dell'eccezione IOException
            throw new PolicyFileNotFoundException("Errore nella lettura del file JSON");
        }
    }

}
