package it.pagopa.pn.ec.pec.configurationproperties;

import lombok.CustomLog;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.SortedMap;

@ConfigurationProperties(prefix = "pn.ec.pec")
@Validated
@Data
@CustomLog
public class PnPecConfigurationProperties {

    private String attachmentRule;
    private int maxMessageSizeMb;
    @Pattern(regexp = "([A-Za-z]*)|([A-Za-z]*;\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[A-Za-z]*)") //nuovo pattern per provider scrittura
    private String pnPecProviderSwitchWrite;
    @Pattern(regexp = "([A-Za-z]*)|([A-Za-z]*;\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[A-Za-z]*)") //nuovo pattern per providers lettura
    private String pnPecProviderSwitchRead;
    @Pattern(regexp = "(true|false);\\d{4}-\\d{2}-\\true|false|(d{2}T\\d{2}:\\d{2}:\\d{2}Z;(true|false))$")
    private String tipoRicevutaBreve;
    private String tipoRicevutaHeaderName;
    private String tipoRicevutaHeaderValue;

    private SortedMap<DateTime,List<String>> dateProviders;



    //Restituisce lista di provider per intervallo di date corrente (Lista di un elemento per write)
    private List<String> returnPropertyValue(String propertyString) {
        List<String> providers = List.of();
        String[] propertyArray = propertyString.split(";");
        if(propertyArray.length == 1) {
            return List.of(propertyArray[0]);
        } else if(propertyArray.length == 2 || propertyArray.length > 3){
            throw new IllegalArgumentException("Error parsing property values,wrong number of arguments.");
        } else {
            String valueBeforeDate = propertyArray[0];
            String valueAfterDate = propertyArray[2];
            DateTime date = DateTime.parse(propertyArray[1]);
            DateTime now = DateTime.now();
            log.info("PnPecConfigurationProperties.returnPropertyValue() -> Date : {}, Now : {}", date, now);
            if(now.isBefore(date)) {
                return List.of(valueBeforeDate); //modifica provvisoria
            } else {
                return List.of(valueAfterDate); //modifica provvisoria
            }
        }
    }

    public boolean getTipoRicevutaBreve() {
        try{
            return Boolean.parseBoolean(returnPropertyValue(tipoRicevutaBreve).get(0));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing TipoRicevutaBreve value");
        }
    }

    public String getPnPecProviderSwitch() {
        return returnPropertyValue(pnPecProviderSwitchWrite).get(0);
    }

    public List<String> getPnPecProviderSwitchRead() {
        return returnPropertyValue(pnPecProviderSwitchRead);
    }
}