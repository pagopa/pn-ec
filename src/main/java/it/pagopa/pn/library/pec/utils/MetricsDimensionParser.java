package it.pagopa.pn.library.pec.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;

public class MetricsDimensionParser {

    private final ObjectMapper objectMapper;

    public MetricsDimensionParser() {
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows(JsonProcessingException.class)
    public Map<String, Map<String, List<Long>>> parsePecDimensionJson(String dimensionsJson) {
        // Creazione di un TypeReference per indicare il tipo di mappa desiderato
        TypeReference<Map<String, Map<String, List<Long>>>> typeRef = new TypeReference<>() {};

        // Deserializzazione del JSON nella mappa
        return objectMapper.readValue(dimensionsJson, typeRef);

    }

}
