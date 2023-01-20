package it.pagopa.pnec.notificationtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${statemachine.url}")
    String stateMachiUrl;

    public String getStateMachiUrl() {
        return stateMachiUrl;
    }

    public void setStateMachiUrl(String stateMachiUrl) {
        this.stateMachiUrl = stateMachiUrl;
    }

}
