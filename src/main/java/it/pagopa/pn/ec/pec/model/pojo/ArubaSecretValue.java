package it.pagopa.pn.ec.pec.model.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Data
public class ArubaSecretValue {

    @JsonProperty("user")
    String pecUsername;
    @JsonProperty("pass")
    String pecPassword;
}
