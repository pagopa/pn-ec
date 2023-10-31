package it.pagopa.pn.ec.scaricamentoesitipec.model.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.ec.rest.v1.dto.SingleStatusUpdate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancellazioneRicevutePecDto {

    @JsonProperty("detail")
    SingleStatusUpdate singleStatusUpdate;

}
