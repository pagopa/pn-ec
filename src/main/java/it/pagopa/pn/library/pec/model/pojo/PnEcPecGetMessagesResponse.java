package it.pagopa.pn.library.pec.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PnEcPecGetMessagesResponse {

    private PnEcPecListOfMessages pnEcPecListOfMessages;
    private int numOfMessages;

}
