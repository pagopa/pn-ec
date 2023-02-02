package it.pagopa.pn.ec.commons.model.configurationproperties.endpoint;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@Data
@PropertySource("classpath:internal-endpoint.properties")
@ConfigurationProperties(prefix = "safe-storage")
public class UriBuilderEndpoint {

//    <-- ATTACHMENTS -->

    String getFile;

}
