package it.pagopa.pn.ec.pec.configurationproperties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.pec")
@Getter
public class PnPecConfigurationProperties {

    private String attachmentRule;
    private int maxMessageSizeMb;
    private String tipoRicevutaBreve;

}
