package it.pagopa.pn.ec.email.model.pojo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
public class EmailAttach {

	String key;
	String contentType;
	String url;

}
