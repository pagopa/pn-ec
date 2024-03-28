package it.pagopa.pn.ec.commons.model.pojo.email;

import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.mail.Header;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EmailField {

    String msgId;
    @ToString.Exclude
    String from;
    @ToString.Exclude
    String to;
    @ToString.Exclude
    String subject;
    @ToString.Exclude
    String text;
    String contentType;
    @ToString.Exclude
    List<EmailAttachment> emailAttachments;
    @ToString.Exclude
    List<Header> headersList;

}
