package it.pagopa.pn.ec.commons.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class OptionalUtils {

    private OptionalUtils() {
        throw new IllegalStateException("OptionalUtils is a utility class");
    }

    /**
     * Safely gets the first element of a {@link List}
     *
     * @param list a {@code List}
     * @param <E>  type of elements in list
     * @return the first element of list or {@link Optional#empty()} if absent or null
     */
    @Nonnull
    public static <E> Optional<E> getFirstListElement(@Nullable List<E> list) {
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.get(0));

    }
}
