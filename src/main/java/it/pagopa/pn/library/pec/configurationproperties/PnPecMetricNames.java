package it.pagopa.pn.library.pec.configurationproperties;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "library.pec.cloudwatch.metrics")
public class PnPecMetricNames {

    String markMessageAsReadResponseTime;
    String deleteMessageResponseTime;
    String getUnreadMessagesResponseTime;
    String getMessageCountResponseTime;
    String sendMailResponseTime;
    String payloadSizeRange;
    String messageCountRange;

    // Metodo per ottenere tutti i valori come lista
    public List<String> getAllMetrics() {
        return List.of(
                markMessageAsReadResponseTime,
                deleteMessageResponseTime,
                getUnreadMessagesResponseTime,
                getMessageCountResponseTime,
                sendMailResponseTime,
                payloadSizeRange,
                messageCountRange
        );
    }

}
