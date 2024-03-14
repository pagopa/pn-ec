package it.pagopa.pn.ec.pec.configurationproperties;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import java.util.*;

@ConfigurationProperties(prefix = "pn.ec.pec")
@Validated
@Getter
@Setter
@CustomLog
public class PnPecConfigurationProperties {

    private String attachmentRule;
    private int maxMessageSizeMb;
    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+)(?:,(?:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+))*")
    private String pnPecProviderSwitchWrite;

    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+(?:\\|[a-zA-Z]+)*)((?:,\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+(?:\\|[a-zA-Z]+)*))*")
    private String pnPecProviderSwitchRead;

    @Pattern(regexp = "(true|false);\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;(true|false)")
    private String tipoRicevutaBreve;
    private String tipoRicevutaHeaderName;
    private String tipoRicevutaHeaderValue;


    private TreeMap<DateTime, List<String>> splitDateProviders(String propertyString) {
        TreeMap<DateTime, List<String>> dateProviders = new TreeMap<DateTime, List<String>>();
        String[] propertyArray = propertyString.split(",");
        for (String property : propertyArray) {
            DateTime key;
            List<String> values = new ArrayList<String>();
            String[] propertyBase = property.split(";");
            key = DateTime.parse(propertyBase[0]);
            String[] providerProperty = propertyBase[1].split("\\|");
            for (String provider : providerProperty) {
                values.add(provider.toLowerCase());
            }
            dateProviders.put(key, values);
        }
        return dateProviders;
    }

    private List<String> returnPropertyValue(String propertyString) {
        List<String> providers = new ArrayList<>();
        SortedMap<DateTime, List<String>> dateProviderMap = splitDateProviders(propertyString).descendingMap();
        DateTime now = DateTime.now();

        for (Map.Entry<DateTime, List<String>> entry : dateProviderMap.entrySet()) {
            if (entry.getKey().isBefore(now)) {
                providers.addAll(entry.getValue());
                break;
            }
        }
        return providers;
    }

    private String parseTipoRicevutaBreveProp(String propertyString) {
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
            log.debug("PnPecConfigurationProperties.returnPropertyValue() -> Date : {}, Now : {}", date, now);
            if(now.isBefore(date)) {
                return valueBeforeDate;
            } else {
                return valueAfterDate;
            }
        }
    }

    public boolean getTipoRicevutaBreve() {
        try{
            return Boolean.parseBoolean(parseTipoRicevutaBreveProp(tipoRicevutaBreve));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing TipoRicevutaBreve value");
        }
    }

    public String getPnPecProviderSwitchWrite() {
        return returnPropertyValue(pnPecProviderSwitchWrite).get(0);
    }

    public List<String> getPnPecProviderSwitchRead() {
        return returnPropertyValue(pnPecProviderSwitchRead);
    }
}