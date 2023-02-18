package it.pagopa.pn.ec.commons.exception.httpstatuscode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public class Generic500ErrorException extends RuntimeException{

    final String title;
    final String details;
}
