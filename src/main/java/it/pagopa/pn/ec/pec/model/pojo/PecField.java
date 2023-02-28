package it.pagopa.pn.ec.pec.model.pojo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class PecField {

    String from;
    String to;
    String configSet;
    String subject;
    String htmlBody;
    String textBody;
    List<String> attachmentsUrl;
}
