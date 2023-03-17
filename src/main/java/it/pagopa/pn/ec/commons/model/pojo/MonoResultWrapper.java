package it.pagopa.pn.ec.commons.model.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class MonoResultWrapper <T> {

    T result;

    public boolean isNotEmpty() {
        return result != null;
    }
}
