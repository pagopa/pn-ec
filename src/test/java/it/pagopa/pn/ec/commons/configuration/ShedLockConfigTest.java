package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.configuration.scheduler.ShedLockConfig;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest(classes = ShedLockConfig.class)
@EnableConfigurationProperties(ShedLockConfig.class)
@TestPropertySource(properties = {
        "pn.ec.shedlock.table-name=pn-EcShedlockCounter"
})
class ShedLockConfigTest {
    @Autowired
    private ShedLockConfig shedLockConfig;

    @Autowired
    private LockProvider lockProvider;

    @MockitoBean
    private DynamoDbClient dynamoDbClient;

    @Test
    void testLockProviderPropertyLoaded() {
        assertNotNull(lockProvider, "LockProvider non dovrebbe essere null");
        String tableName = shedLockConfig.getTableName();
        log.info("TableName letta dalle properties: {}", tableName);
        assertEquals("pn-EcShedlockCounter", tableName, "La property deve essere passata correttamente");
    }
}