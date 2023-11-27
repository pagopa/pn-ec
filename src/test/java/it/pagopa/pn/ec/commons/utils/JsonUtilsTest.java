package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.exception.JsonStringToObjectException;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public class JsonUtilsTest {

    private final ObjectMapper objectMapperMock = mock(ObjectMapper.class);
    private final JsonUtils jsonUtils = new JsonUtils(objectMapperMock);

    @Test
    public void convertJsonStringToObjectTestOk() throws JsonStringToObjectException, JsonProcessingException {
        String jsonString = "{\"string\":\"test\",\"number\":1}";
        TestClass expectedTestClass = new TestClass("test", 1);

        when(objectMapperMock.readValue(jsonString, TestClass.class)).thenReturn(expectedTestClass);

        TestClass actualTestClass = jsonUtils.convertJsonStringToObject(jsonString, TestClass.class);

        assertEquals(expectedTestClass, actualTestClass);
    }

    @Test
    public void convertJsonStringToObjectTestKO() throws JsonProcessingException {
        String jsonString = "{\"string\":\"test\",\"number\":1}";

        when(objectMapperMock.readValue(jsonString, TestClass.class)).thenThrow(JsonProcessingException.class);

        JsonStringToObjectException jsonStringToObjectException = null;

        try {
            jsonUtils.convertJsonStringToObject(jsonString, TestClass.class);
        } catch (JsonStringToObjectException e) {
            jsonStringToObjectException = e;
        }

        assertEquals(JsonStringToObjectException.class, jsonStringToObjectException.getClass());
    }

    private static class TestClass {
        private String string;
        private int number;

        public TestClass(String string, int number) {
            this.string = string;
            this.number = number;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestClass testClass = (TestClass) obj;
            return number == testClass.number && Objects.equals(string, testClass.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(string, number);
        }
    }
}