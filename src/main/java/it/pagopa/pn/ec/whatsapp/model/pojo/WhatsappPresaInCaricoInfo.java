package it.pagopa.pn.ec.whatsapp.model.pojo;

import it.pagopa.pn.ec.commons.model.pojo.request.PresaInCaricoInfo;
import it.pagopa.pn.ec.rest.v1.dto.DigitalCourtesyWhatsappRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class WhatsappPresaInCaricoInfo extends PresaInCaricoInfo {

    DigitalCourtesyWhatsappRequest digitalCourtesyWhatsappRequest;
}
