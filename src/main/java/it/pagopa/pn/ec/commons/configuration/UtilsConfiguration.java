package it.pagopa.pn.ec.commons.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class UtilsConfiguration {

    @Bean
    public Random random() {
        return new Random();
    }
}
