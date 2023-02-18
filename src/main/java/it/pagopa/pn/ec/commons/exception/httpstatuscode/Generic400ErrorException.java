package it.pagopa.pn.ec.commons.exception.httpstatuscode;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public class Generic400ErrorException extends RuntimeException{

    final String title;
    final String details;
}
