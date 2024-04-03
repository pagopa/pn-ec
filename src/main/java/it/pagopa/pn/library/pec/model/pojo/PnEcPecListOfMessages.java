package it.pagopa.pn.library.pec.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PnEcPecListOfMessages {

    private List<PnEcPecMessage> messages;

}
