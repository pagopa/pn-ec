package it.pagopa.pn.ec.commons.configuration;

import lombok.CustomLog;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;

@CustomLog
@Configuration
@Getter
public class RicezioneEsitiCartaceoConfiguration {
    @Getter
    @Value("${ricezione-esiti-cartaceo.consider-event-without-sent-status-as-booked}")
    private boolean considerEventsWithoutStatusAsBooked;
    @Value("${ricezione-esiti-cartaceo.duplicates-check}")
    private String duplicatesCheck;
    @Getter
    @Value("${ricezione-esiti-cartaceo.allowed-future-offset-duration}")
    private Duration offsetDuration;

    @Getter
    private String[] productTypesToCheck;

    @PostConstruct
    public void init() {
        this.productTypesToCheck = this.duplicatesCheck.split(";");
        this.offsetDuration = this.offsetDuration == null ? Duration.ofSeconds(-1) : this.offsetDuration;
    }

}
