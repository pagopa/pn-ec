package it.pagopa.pn.ec.commons.model.pojo.email;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.io.OutputStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class EmailAttachment {

    String nameWithExtension;
    @ToString.Exclude
    OutputStream content;
    String url;
}
