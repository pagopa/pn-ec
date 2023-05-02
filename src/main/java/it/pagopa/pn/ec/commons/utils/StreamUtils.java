package it.pagopa.pn.ec.commons.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class StreamUtils {

    private StreamUtils() {
        throw new IllegalStateException("StreamUtils is a utils class");
    }

    public static <T> Stream<T> getStreamOfNullableList(List<T> nullableList) {
        return Stream.ofNullable(nullableList).flatMap(Collection::stream);
    }
}
