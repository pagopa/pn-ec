package it.pagopa.pn.ec.commons.configuration.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.ec.shedlock")
@Slf4j
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "${pn.ec.shedlock.lockAtMostFor}")
public class ShedLockConfig {
    private String tableName;

    @PostConstruct
    public void init() {
        log.info("TableName configurata: {}", tableName);
    }

    @Bean
    public LockProvider lockProvider(DynamoDbClient dynamoDbClient) {
        DynamoDBLockProvider provider = new DynamoDBLockProvider(dynamoDbClient, tableName);
        log.info("DynamoDBLockProvider creato con successo: {}", provider);
        return provider;
    }

}
