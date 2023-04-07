package it.pagopa.pn.ec.commons.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

@Configuration
public class UtilsConfiguration {
    @Bean
    public Random random() throws NoSuchAlgorithmException {
        return SecureRandom.getInstanceStrong();
    }
}
