package it.pagopa.pn.library.pec.pojo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class PnGetMessagesResponse {

    PnListOfMessages pnListOfMessages;
    int numOfMessages;

}
