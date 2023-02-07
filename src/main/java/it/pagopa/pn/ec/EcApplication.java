package it.pagopa.pn.ec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ConfigurationPropertiesScan

// <-- COMMONS -->
// AWS CONFIGURATION
@PropertySource("classpath:commons/aws-configuration.properties")
// INTERNAL ENDPOINTS
@PropertySource("classpath:commons/internal-endpoint.properties")

//  <-- REPOSITORY MANAGER -->
// DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")
@Slf4j
public class EcApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcApplication.class, args);
    }
}
