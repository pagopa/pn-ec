package it.pagopa.pn.ec.commons.configuration.ses;

import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@CustomLog
@ConfigurationProperties(prefix="pn.ec.ses")
@Validated
@Getter
@Setter
@Component
public class SesConfigurationProperties {

    private String eventsListDefault;

}