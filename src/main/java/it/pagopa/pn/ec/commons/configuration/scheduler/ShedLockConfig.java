package it.pagopa.pn.ec.commons.configuration.scheduler;

import it.pagopa.pn.ec.configurationproperties.PnEcConfig;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.dynamodb2.DynamoDBLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Slf4j
@EnableSchedulerLock(defaultLockAtMostFor = "${pn.ec.commons.shedlock.lock-at-most-for}")
@ConditionalOnProperty(
        name = "pn.ec.feature.flag.cartaceo.consolidatore",
        havingValue = "true",
        matchIfMissing = false
)
public class ShedLockConfig {
    private final String tableName;

    public ShedLockConfig(PnEcConfig pnEcConfig) {
        this.tableName = pnEcConfig.getCommons().getShedlock().getTableName();
    }

    public String getTableName() {
        return tableName;
    }

    @Bean
    public LockProvider lockProvider(DynamoDbClient dynamoDbClient) {
        return new DynamoDBLockProvider(dynamoDbClient, tableName, "name");
    }

}
