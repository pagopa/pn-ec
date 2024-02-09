package it.pagopa.pn.ec.pec.configurationproperties;

import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "pn.ec.pec")
@Validated
@Data
public class PnPecConfigurationProperties {

    private String attachmentRule;
    private int maxMessageSizeMb;
    @Pattern(regexp = "(true|false|(true|false);\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;(true|false))$")
    private String tipoRicevutaBreve;
    private String tipoRicevutaHeaderName;
    private String tipoRicevutaHeaderValue;


    private String returnPropertyValue(String propertyString) {
        String[] propertyArray = propertyString.split(";");
        if(propertyArray.length == 1) {
            return propertyArray[0];
        } else if(propertyArray.length == 2 || propertyArray.length > 3){
            throw new IllegalArgumentException("Error parsing property values,wrong number of arguments.");
        } else {
            String valueBeforeDate = propertyArray[0];
            String valueAfterDate = propertyArray[2];
            DateTime date = DateTime.parse(propertyArray[1]);
            DateTime now = DateTime.now();
            if(now.isBefore(date)) {
                return valueBeforeDate;
            } else {
                return valueAfterDate;
            }
        }
    }

    public boolean getTipoRicevutaBreve() {
        try{
            return Boolean.parseBoolean(returnPropertyValue(tipoRicevutaBreve));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing TipoRicevutaBreve value");
        }
    }
}