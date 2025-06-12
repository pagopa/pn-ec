package it.pagopa.pn.ec.commons.configuration.sqs;


import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;


@CustomLog
@ConfigurationProperties(prefix="pn.ec.sqs.timeout")
@Validated
@Getter
@Setter
@Component
public class SqsTimeoutConfigurationProperties {

    @Min(0)
    @Max(99)
    private int percent;
    private long defaultSeconds;
    private List<String> managedQueues;

}
