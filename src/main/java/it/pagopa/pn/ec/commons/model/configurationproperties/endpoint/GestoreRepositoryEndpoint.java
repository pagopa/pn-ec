package it.pagopa.pn.ec.commons.model.configurationproperties.endpoint;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Component
@Data
@PropertySource("classpath:internal-endpoint.properties")
@ConfigurationProperties(prefix = "gestore-repository")
public class GestoreRepositoryEndpoint {

    String basePath;

//  <-- CLIENT CONFIGURATION -->
    String getClientConfiguration;
    String postClientConfiguration;
    String putClientConfiguration;
    String deleteClientConfiguration;

//  <-- REQUEST -->
    String getRequest;
    String postRequest;
    String patchRequest;
    String deleteRequest;
}
