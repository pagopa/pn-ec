package it.pagopa.pn.ec.email.model.dto.sns;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Data
@EqualsAndHashCode
@SuperBuilder
public class SnsRawMessageDto {

        private String type;
        private String message;

}
