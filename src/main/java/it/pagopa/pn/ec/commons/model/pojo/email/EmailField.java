package it.pagopa.pn.ec.commons.model.pojo.email;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
public class EmailField {

    String msgId;
    String from;
    String to;
    String subject;
    String text;
    String contentType;
    List<String> attachmentsUrls;
    List<Attachment> outputStreamAttachments;
}
