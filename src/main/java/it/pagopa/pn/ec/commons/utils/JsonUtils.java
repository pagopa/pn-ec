package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.JsonStringToObjectException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonUtils {

    private final ObjectMapper objectMapper;

    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T convertJsonStringToObject(String jsonString, Class<T> classToMap) throws JsonStringToObjectException {
        try {
            return objectMapper.readValue(jsonString, classToMap);
        } catch (JsonProcessingException e) {
            throw new JsonStringToObjectException(jsonString, classToMap);
        }
    }

    public Map<String, List<String>> convertJsonToMap(String jsonString) throws JsonStringToObjectException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        try {
            map = mapper.readValue(jsonString, new TypeReference<Map<String, List<String>>>(){});
        } catch (JsonProcessingException e) {
            throw new JsonStringToObjectException(jsonString, Map.class);
        }
        return map;
    }


}
