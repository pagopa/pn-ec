package it.pagopa.pn.ec.email.model.pojo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class EmailField {

	String from;
	String to;
	String configSet;
	String subject;
	String htmlBody;
	String textBody;

}
