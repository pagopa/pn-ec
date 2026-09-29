package it.pagopa.pn.ec;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {"it.pagopa.pn.ec", "it.pagopa.pn.library.pec","it.pagopa.pn.template"})
@ConfigurationPropertiesScan(basePackages = {"it.pagopa.pn.ec", "it.pagopa.pn.library.pec","it.pagopa.pn.template"})
public class EcApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EcApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }
}
