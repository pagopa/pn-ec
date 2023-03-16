package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.JsonStringToObjectException;
import org.springframework.stereotype.Component;

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
}
